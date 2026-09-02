# 📦 Event-Driven Order Service

Bu proje, **Event-Driven Architecture (Olay Güdümlü Mimari) prensiplerinin Spring Boot ve Apache Kafka kullanılarak** gerçek hayata yakın bir e-ticaret sipariş senaryosunda uygulanmasını göstermek amacıyla geliştirilmiştir.

Bir e-ticaret uygulamasında kullanıcının sipariş oluşturmasıyla başlayan ve arka planda birden fazla servisin asenkron olarak tetiklendiği bir yapı ele alınmıştır. Tüm süreç, servisler arası kuplajı (coupling) minimuma indirmek amacıyla **Event (Olay)** akışları üzerinden yönetilir.

## 🎯 Projenin Amacı

Monolitik veya senkron mikroservis yapılarında bir sipariş verildiğinde; ödeme, stok ve bildirim işlemleri ardışık (senkron HTTP/REST çağrıları ile) yapılır. Bu durum, servislerden biri çöktüğünde tüm sipariş akışının kesilmesine (Single Point of Failure) ve ağ gecikmelerine neden olur.

Bu projede ise asenkron iletişim hedeflenmiştir. Süreç şu event'ler ile yönetilir:
* 🛒 **OrderCreated:** Siparişin ilk alındığı andaki event.
* 💳 **PaymentCompleted:** Ödeme işleminin başarıyla tamamlandığı event.
* 📦 **StockReserved:** Ürün envanterinin başarıyla düşüldüğü event.
* 🎉 **OrderCompleted:** Tüm onaylar geldikten sonra siparişin kesinleştiği event.

Bu iş kuralları Kafka Topic'leri ve bağımsız Consumer yapıları ile izole edilerek sistemin **yüksek erişilebilir (Highly Available), esnek ve ölçeklenebilir (Scalable)** olması hedeflenmiştir.

---

## 🧩 Event-Driven Architecture (EDA) Nedir?

**Event-Driven Architecture**, sistemdeki durum değişikliklerinin (event) üretilmesi, tespit edilmesi ve tüketilmesi üzerine kurulu bir mimari modeldir. 

Bu yaklaşımda servisler birbirlerinin API endpoint'lerini (URL) doğrudan çağırmazlar. Bunun yerine bir servis yaptığı işlemi ortak bir mesaj kuyruğuna (Message Broker) "X olayı gerçekleşti" şeklinde raporlar. O olayla ilgilenen diğer servisler ise kuyruğu dinleyerek (Subscribe) kendi üzerlerine düşen görevi yerine getirir.

Projelerimizde merkezi Event Broker **Apache Kafka**'dır:

```text
OrderService (Producer)
      │
      └───► [ Kafka Topic: order-events ]
                 │
                 ├───► PaymentService (Consumer)
                 │
                 ├───► StockService (Consumer)
                 │
                 └───► NotificationService (Consumer)
```

---

## 💡 Neden Event-Driven Mimari Kullanıldı?

E-ticaret gibi anlık trafiğin çok yüksek olabileceği sistemlerde asenkron yapı hayati önem taşır.

* **Loose Coupling (Gevşek Bağlılık):** `Order Service`, ödeme veya stok servisinin o an ayakta olup olmadığını bilmek zorunda değildir. Mesajı Kafka'ya bırakır ve işine devam eder.
* **Asynchronous Communication:** Kullanıcı sipariş butonuna bastığında ödemenin bitmesini, stoğun düşmesini ve SMS gelmesini beklemez. Siparişin alındığı bilgisini anında görür, işlemler arka planda akar.
* **Fault Tolerance (Hata Toleransı):** Ödeme servisi o an çökmüş olsa bile Kafka mesajları saklar. Ödeme servisi ayağa kalktığı anda kaldığı yerden mesajları işlemeye devam eder; veri kaybı yaşanmaz.
* **Scalability (Ölçeklenebilirlik):** Sipariş sayısı çok arttığında, sistemi yavaşlatmadan sadece `Stock Service` veya `Payment Service` instance sayılarını (Consumer count) artırarak darboğazlar kolayca çözülebilir.

---

## ❌ Event-Driven Mimari Kullanılmasaydı

Süreç tek bir servis içinde veya senkron HTTP (Feign Client / RestTemplate) çağrıları ile yönetilseydi:

```java
public void createOrder(OrderRequest request) {
    orderRepository.save(order); // 1. DB'ye kaydet
    paymentClient.processPayment(request); // 2. Ödeme servisine HTTP isteği at (Bekle!)
    stockClient.reduceStock(request); // 3. Stok servisine HTTP isteği at (Bekle!)
    notificationClient.sendSms(request); // 4. Bildirim servisine HTTP isteği at (Bekle!)
}
```

* **Problem 1:** Stok servisi 5 saniye geç cevap verirse, kullanıcı ekranda 5 saniye boyunca yükleme ikonu görür.
* **Problem 2:** Bildirim servisi hata verirse tüm transaction rollback olabilir ve kullanıcının ödemesi alınmışken siparişi iptal durumuna düşebilir.
* **Problem 3:** Yeni bir `Fraud (Sahtecilik) Kontrol Servisi` eklenmek istendiğinde, ana sipariş kodunun içine girip yeni bir bağımlılık eklemek ve mevcut kodu değiştirmek gerekir (**Open/Closed Principle** ihlali).

---

## 🏗️ Proje Mimarisi

```text
   Client (Postman/Frontend)
           │
           ▼
    OrderController
           │
           ▼
    OrderServiceImpl
           │
    ┌──────┴──────────────┐
    ▼                     ▼
PostgreSQL         Kafka Producer
(Status: PENDING)         │
                          ▼
                  [ Apache Kafka ]
               (Topic: order-events)
                          │
         ┌────────────────┼────────────────┐
         ▼                ▼                ▼
   PaymentService    StockService    NotificationService
    (Consumer)        (Consumer)         (Consumer)
         │                │
         └────────┬───────┘
                  ▼
            Kafka Producer
                  │
                  ▼
          [ Apache Kafka ]
       (Topic: status-updates)
                  │
                  ▼
            Order Consumer
                  │
                  ▼
             PostgreSQL
         (Status: COMPLETED)
```

### 📁 Klasör ve Paket Yapısı

```text
org.example.orderservice
│
├── config                 # Kafka Producer/Consumer ve App Yapılandırmaları
├── controller             # REST API Endpoint'leri (OrderController)
├── dto                    # Request ve Response Veri Transfer Nesneleri (DTO)
├── entity                 # Veritabanı Modelleri (OrderEntity)
├── enums                  # Sipariş Durumları (OrderStatus: PENDING, COMPLETED, FAILED)
├── exception              # Merkezi Hata Yönetimi Sınıfları (OrderNotFoundException)
├── repository             # Spring Data JPA Veritabanı Erişim Katmanı
└── service                # İş Mantığı Katmanı (OrderService, OrderServiceImpl)
```

---

## 🔄 Sipariş ve Event Akışı

Bir sipariş tetiklendiğinde sistem sırasıyla şu adımları izler:

1. **İstek Kabulü:** Client, `POST /api/orders` endpoint'ine sipariş isteği gönderir.
2. **İlk Kayıt:** `OrderServiceImpl` siparişi veri tabanına **`PENDING`** (Beklemede) statüsüyle kaydeder.
3. **Event Fırlatma:** Sipariş kaydedildiği an, Kafka'nın `order-created-events` topiğine bir `OrderCreatedEvent` mesajı basılır.
4. **Paralel İşleme:** 
   * `Payment Service` bu eventi dinler, ödemeyi simüle eder ve başarılıysa `PaymentCompletedEvent` fırlatır.
   * `Stock Service` aynı eventi dinler, stoğu rezerve eder ve `StockReservedEvent` fırlatır.
   * `Notification Service` eventi dinler ve kullanıcıya "Siparişiniz alındı" mail/SMS logunu basar.
5. **Kapanış ve Onay:** `Order Service` gelen başarı eventlerini dinleyerek veritabanındaki sipariş durumunu **`COMPLETED`** olarak günceller.

---

## 🛠️ Kullanılan Teknolojiler

| Teknoloji         | Kullanım Amacı                   |
| ----------------- | -------------------------------- |
| Java 17           | Modern Nesne Yönelimli Backend Geliştirme |
| Spring Boot 3.x   | Ana Uygulama Çatısı ve Bağımlılık Yönetimi |
| Apache Kafka      | Dağıtık Event-Driven Mesaj Broker Altyapısı |
| Kafka UI          | Kafka Cluster, Topic ve Mesaj İzleme Arayüzü |
| Spring Data JPA   | PostgreSQL Veritabanı ORM İşlemleri |
| PostgreSQL 16     | İlişkisel Veritabanı (Sipariş Kayıtları) |
| Lombok            | Boilerplate (Getter/Setter/Builder) Kod Azaltımı |
| Bean Validation   | Request Body Doğrulama Kuralları (@NotNull, @Min) |
| JUnit 5           | Birim (Unit) Test Altyapısı |
| Mockito           | Bağımlılıkların İzole Edilmesi ve Mock Yapısı |
| MockMvc           | Controller Katmanı HTTP İstek Testleri |
| Docker Compose    | Altyapı Servislerinin Tek Tuşla Konteyner Yönetimi |

---

## 🌐 API Endpoints

### POST `/api/orders`

Sistem üzerinde yeni bir asenkron sipariş akışı başlatır.

**Request Body:**
```json
{
  "customerId": 1,
  "productId": 100,
  "quantity": 2,
  "totalPrice": 250.00
}
```

**Response Body (201 Created):**
```json
{
  "id": 10,
  "customerId": 1,
  "productId": 100,
  "quantity": 2,
  "totalPrice": 250.00,
  "status": "PENDING",
  "createdAt": "2026-09-02"
}
```

---

### GET `/api/orders`
Sistemdeki tüm sipariş geçmişini listeler.

---

### GET `/api/orders/{id}`
Verilen benzersiz ID bilgisine ait siparişin detayını ve o anki güncel durumunu (`PENDING`, `COMPLETED`) getirir.

---

## ✅ Validation & Global Exception Handling

Uygulamaya giren veriler `jakarta.validation` kuralları ile kapıda denetlenir. Geçersiz isteklerde `GlobalExceptionHandler` devreye girerek istemciye standart bir hata şeması döner.

* **Validasyon Kuralları:** `@Min(1)` ile ürün adedi kontrolü, `@DecimalMin("0.0")` ile fiyat kontrolü sağlanır.
* **Hata Yönetimi:** Olmayan bir sipariş istendiğinde `OrderNotFoundException` fırlatılır ve sistem bunu otomatik olarak `404 Not Found` HTTP koduyla sarmallar:

**Örnek Hata Çıktısı (400 Bad Request):**
```json
{
  "timestamp": "2026-09-02T15:38:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validasyon hatası gerçekleşti.",
  "path": "/api/orders",
  "validationErrors": {
    "quantity": "Ürün adedi en az 1 olmalıdır.",
    "customerId": "Müşteri ID boş olamaz."
  }
}
```

---
