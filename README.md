# Limore Queue Assistant & Keeper (Android Go Edition)

Aplikasi pendamping khusus untuk pengguna Redmi A2 / Android Go yang sering mengalami masalah antrean Limore Cloud Game reload atau tertutup saat ditinggal.

## ⚡ Fitur Utama v1.1.0 (Shizuku 1-Click Anti-Kill):
1. **1-Click Shizuku Anti-Kill**:
   Tidak perlu ketik perintah ADB manual di PC atau LADB. Cukup buka Shizuku, izinkan aplikasi ini, lalu klik tombol ungu di aplikasi. Semua perintah optimasi sistem akan otomatis dijalankan:
   - `cmd appops set com.lingwoyun.limore RUN_IN_BACKGROUND allow`
   - `dumpsys deviceidle whitelist +com.lingwoyun.limore`
   - Matikan Phantom Process Killer Android
   - Paksa aktifkan Freeform Floating Window & Resizable
2. **Keep-Alive Foreground Service & Partial WakeLock**:
   Mencegah CPU Android masuk ke mode *Deep Sleep / Doze*, menjaga jalur koneksi socket antrean tetap aktif di latar belakang.
3. **NotificationListenerService & Smart Alarm**:
   Mendengarkan notifikasi secara *real-time* dari server Limore:
   - Membunyikan suara alarm kencang meskipun HP dalam mode hening.
   - Menggetarkan HP berulang-ulang sampai dimatikan.
   - Otomatis meluncurkan Limore ke layar depan agar Anda tidak kehabisan batas waktu tunggu (30 detik).

---

## 📲 Download APK:
File APK langsung di-build lewat GitHub Actions:
- **[Download APK Terbaru (GitHub Release)](https://github.com/xykalnotkel/LimoreQueueKeeper/releases)**

---

Built with ❤️ for Redmi A2 (Android Go) & XyVerse Ecosystem.
