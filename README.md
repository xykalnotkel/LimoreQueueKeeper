# Limore Queue Assistant & Keeper (Android Go Edition)

Aplikasi pendamping khusus untuk pengguna Redmi A2 / Android Go yang sering mengalami masalah antrean Limore Cloud Game reload atau tertutup saat ditinggal.

## 🛠️ Masalah Teknis di Android Go:
Android Go memiliki fitur keamanan **Low Memory Killer Daemon (LMKD)** dan **App Sandboxing**:
1. Satu aplikasi pihak ketiga **tidak diizinkan oleh sistem Android** untuk secara langsung memodifikasi memori aplikasi lain (`oom_score_adj`) tanpa akses Root/ADB.
2. Setiap kali Anda membuka aplikasi berat (YouTube/TikTok), sistem Android Go secara paksa mematikan aktivitas yang berada di latar belakang (*background*).
3. Akibatnya, socket antrean Limore terputus dan aplikasi memuat ulang (*reload*) dari awal.

## 💡 Solusi yang Diterapkan di Aplikasi Ini:
1. **Keep-Alive Foreground Service & Partial WakeLock**:
   Mencegah CPU Android masuk ke mode *Deep Sleep / Doze*, menjaga jalur koneksi data tetap aktif di latar belakang.
2. **NotificationListenerService (Deteksi Otomatis)**:
   Mendengarkan notifikasi secara *real-time* dari package `com.lingwoyun.limore`. Begitu ada panggilan/giliran masuk dari server Limore:
   - Membunyikan suara alarm kencang (stream Alarm) meskipun HP dalam mode hening.
   - Menggetarkan HP berulang-ulang sampai dimatikan.
   - **Otomatis meluncurkan Limore ke layar depan (Foreground)** seketika itu juga agar Anda tidak kehabisan batas waktu tunggu.

---

## 📱 Cara Kompilasi / Pasang Jadi APK:
### Cara 1: Menggunakan Android Studio (PC / Laptop)
1. Buka folder `LimoreQueueAssistant` di Android Studio.
2. Tunggu Gradle Sync selesai.
3. Klik menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Kirim file `.apk` ke Redmi A2 Anda lalu instal.

### Cara 2: Kompilasi Langsung di HP Android
1. Pasang aplikasi **AIDE - IDE for Android Java/C++** dari Play Store / APKMirror.
2. Buka folder proyek ini di AIDE dan jalankan **Build / Run**.

---

## ⚡ Trik Tambahan Khusus Redmi A2 (Biar Limore 100% Gak Reload):
Buka **Opsi Pengembang (Developer Options)**:
1. Aktifkan **"Paksa aktivitas agar dapat diubah ukurannya"** (*Force activities to be resizable*).
2. Aktifkan **"Aktifkan jendela bentuk bebas"** (*Enable freeform windows*).
3. Buka Limore dalam mode *Floating Window / Jendela Melayang* (layar kecil di pojok), lalu buka aplikasi lain di layar utama. Karena Limore tetap ada di layar, Android Go **tidak akan pernah menganggapnya background**, sehingga koneksi antrean tidak akan pernah putus!
