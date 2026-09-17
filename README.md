# Limore Queue Assistant & Keeper (v1.3 Ultra Power Edition)

Aplikasi pendamping khusus untuk pengguna Redmi A2 / Android Go yang sering mengalami masalah antrean Limore Cloud Game reload atau tertutup saat ditinggal.

## 🚀 Apa yang Baru di v1.3:
1. **Perbaikan Deteksi Shizuku (`Sticky Binder Listener` + Provider Eksplisit)**:
   - Memperbaiki bug status Shizuku yang sebelumnya terlambat atau tidak terbaca meskipun Shizuku sudah running. Sekarang menggunakan *Sticky Listener* dan tombol *Refresh Cepat*.
2. **Paksa Limore Mode Jendela Melayang (Dual-Force Freeform)**:
   - Tombol **`🪟 Paksa Limore Mode Freeform`** mengeksekusi dua lapis perintah sekaligus:
     - Lapisan 1: ADB / Shizuku Shell `am start --windowingMode 5 -n <komponen_limore>`.
     - Lapisan 2: Java Reflection `ActivityOptions.setLaunchWindowingMode(5)`.
     - Otomatis membuka Floating Bubble di atas layar.
3. **Ultra Anti-Kill Kernel Daemon (OOM Protector)**:
   - Saat service pemantau aktif, aplikasi secara berkala mengunci prioritas Linux Kernel Limore (`/proc/$PID/oom_score_adj = -900`), menyamakannya dengan prioritas *System Service* agar Low Memory Killer (LMK) dilarang keras membunuhnya.
4. **Modern 3-Tab Bottom Navigation + Floating Bubble Overlay**:
   - Menu Antrean, Anti-Kill, dan Pengaturan lengkap.

---

## 📲 Unduh APK:
- **[Halaman Rilis GitHub](https://github.com/xykalnotkel/LimoreQueueKeeper/releases)**

Built with ❤️ for Redmi A2 (Android Go) & XyVerse Ecosystem.
