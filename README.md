# 📦 order-event-service | Event-Driven Sipariş Sistemi

Bu proje; mikroservis mimarilerinde yaygın olarak kullanılan **Event-Driven Architecture** prensiplerini uygulamak, servisler arası asenkron iletişimi ve veri tutarlılığını (Data Consistency) **Apache Kafka** kullanarak simüle etmek amacıyla geliştirilmiş uçtan uca bir backend sistemidir.

Bir kullanıcının sipariş vermesinden, siparişin onaylanıp bildiriminin gönderilmesine kadar olan tüm süreç Kafka Topic'leri üzerinden asenkron olaylar (events) fırlatılarak yönetilir.

---

## 🏗️ Sistem Mimarisi ve İş Akışı

Sistemde bir sipariş oluşturulduğunda süreç doğrusal (senkron) ilerlemez. Bunun yerine ilgili servisler Kafka üzerinden event'leri dinleyerek (Consume) kendi iş mantıklarını tetikler.

### Olay Akış Adımları (Event Lifecycle)
1. **`OrderCreated`**: Müşteri sipariş verir. Sipariş veri tabanına `PENDING` statüsünde kaydedilir ve Kafka'ya olay fırlatılır.
2. **`PaymentCompleted`**: Ödeme servisi olayı yakalar, ödemeyi işler ve başarılıysa onay olayını yayınlar.
3. **`StockReserved`**: Stok servisi olayı yakalar, ilgili ürünün envanterini düşer ve stok rezerve olayını yayınlar.
4. **`OrderCompleted`**: Ödeme ve stok onayları geldikten sonra `Order Service` sipariş durumunu `COMPLETED` olarak günceller.
5. **`NotificationService`**: Sürecin her adımında bağımsız olarak kuyruğu dinler ve simüle edilmiş e-posta/SMS bildirimlerini basar.

---

## 🛠️ Kullanılan Teknolojiler

* **Dil:** Java 17 / 21
* **Framework:** Spring Boot 3.x
* **Event Broker:** Apache Kafka (KRaft Mode)
* **Veritabanı:** PostgreSQL 16
* **Test:** JUnit 5, Mockito, MockMvc
* **Konteynerleştirme:** Docker & Docker Compose
* **Dokümantasyon:** Swagger UI (Springdoc OpenAPI)

---
