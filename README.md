# Update Home & Screensaver (CleanYourTV Launcher)
## Rilis Kustomisasi untuk Android TV (AX512 & B866F)

Repositori ini memuat pembaruan lengkap untuk antarmuka **Home** dan fitur **Screensaver Waktu Sholat (PrayerDream)** pada CleanYourTV Launcher (`com.gathin.tvhome`). Pembaruan ini telah diuji dan berjalan stabil pada perangkat **SDMC AX512 (Android 9)** serta **ZTE B866F (Android 14)**.

---

## 1. Rincian Perubahan Tampilan Home

### A. Indikator Sistem Melingkar (Circular Meters 60×60 dp)
- Menggantikan meteran batang vertikal lama dengan **3 indikator melingkar (Circular Progress Arc)** berukuran **60×60 dp**.
- Menampilkan persentase (`%`) tepat di tengah masing-masing ikon:
  - **CPU**: Ikon prosesor dengan busur progres warna emas (`#E9CB7E`).
  - **RAM**: Ikon chip memori dengan busur progres warna biru (`#64B5F6`).
  - **Penyimpanan**: Ikon drive penyimpanan dengan busur progres warna hijau mint (`#81C784`).
- Label judul **"SISTEM"** dan garis vertikal pembatas telah dihilangkan sehingga tampilan lebih bersih dan modern.
- Posisi panel indikator dinaikkan dengan jarak tepat **25 dp** di bawah tombol *Setelan*.

### B. Penyelarasan Panel Jadwal Sholat & Hijriyah
- Bagian atas kartu **JADWAL SHOLAT** (`prayerPanel`) disejajarkan secara horizontal dengan bagian atas kartu **HIJRIYAH** (`hijriSection`) pada `layout_marginTop="190dp"` (Y = ~382 px pada layar 1080p).

### C. Penyesuaian Dock Aplikasi & Hero Title
- Letak pita dock aplikasi dinaikkan sebesar **10 dp** (`DOCK_DROP_DP = 30`).
- Judul hero (*Dopamine TV*) di atas dock diturunkan sebesar **20 dp** sehingga jarak antara nama aplikasi dan dock menjadi lebih rapat dan proporsional.

---

## 2. Fitur Screensaver Islami (`PrayerDream`)

Screensaver ini diimplementasikan sebagai `DreamService` Android TV bawaan yang dapat dipilih melalui menu Pengaturan TV atau diaktifkan lewat ADB:
```shell
settings put secure screensaver_components com.gathin.tvhome/.PrayerDream
settings put secure screensaver_enabled 1
```

### Fitur Utama Screensaver:
1. **Jam & Kalender Lengkap**:
   - Jam digital besar serif tebal berwarna emas.
   - Hari & pasaran Jawa (Weton), misal: `SABTU KLIWON`.
   - Tanggal Masehi dan tanggal Hijriah KHGT Muhammadiyah (misal: `14 Jumadilakhir 1448 H`).
2. **Kartu 5 Waktu Sholat**:
   - Menampilkan Subuh, Dzuhur, Ashar, Maghrib, dan Isya.
   - Waktu sholat berikutnya otomatis disorot dengan bingkai emas dan label hitung mundur (*misal: "Dzuhur · 4 jam 10 menit lagi"*).
3. **Rotasi Ayat Al-Qur'an**:
   - Menampilkan teks Arab ayat pilihan beserta terjemahan bahasa Indonesia dan nomor surah/ayat dari berkas `ayat.json`.
4. **Pencegahan Burn-In Layar**:
   - Setiap 20 detik, seluruh konten layar digeser halus (*nudge drift*) secara acak sebesar ±1% dari ukuran layar untuk melindungi panel TV (OLED/LED) dari risiko *image retention/burn-in*.

---

## 3. Struktur Berkas dalam Paket Ini

```text
github-export-ax512/
├── README.md                      <-- Dokumentasi ini
├── screenshot-home.png            <-- Tangkapan layar tampilan Home
├── release-apk/                   <-- File siap pasang
│   ├── app-release.apk            <-- APK launcher terkompilasi
│   └── CleanYourTV-system-3.4.zip <-- Modul Magisk (system app)
├── changed-files/                 <-- Berkas yang diubah/ditambahkan saja
│   ├── home/
│   │   ├── java/
│   │   │   ├── CircularMeterView.java  <-- Widget indikator lingkaran
│   │   │   ├── MainActivity.java       <-- Logika persentase, hero, dan wiring
│   │   │   ├── AppAdapter.java         <-- Kenaikan posisi dock
│   │   │   ├── Weton.java              <-- Kalender Jawa
│   │   │   ├── Hijri.java              <-- Kalender Hijriyah
│   │   │   ├── PrayerTimes.java        <-- Perhitungan waktu sholat
│   │   │   ├── PrayerDay.java
│   │   │   ├── Weather.java            <-- Data cuaca & kualitas udara AQI
│   │   │   ├── Azan.java
│   │   │   ├── AzanAlarm.java
│   │   │   └── AzanOverlay.java        <-- Overlay notifikasi & pemutar suara adzan
│   │   └── res/
│   │       ├── layout/activity_main.xml
│   │       └── drawable/ (ic_cpu.xml, ic_ram.xml, ic_storage.xml, meter_*.xml)
│   └── screensaver/
│       ├── java/
│       │   ├── PrayerDream.java        <-- Service screensaver
│       │   ├── PrayerScene.java        <-- Pengatur tata letak & teks
│       │   ├── IslamicPattern.java     <-- Canvas pola bintang 8
│       │   └── ClockDream.java
│       └── res/
│           ├── layout/dream_prayer.xml
│           ├── drawable/prayer_card.xml, prayer_cell_next.xml
│           └── raw/ayat.json           <-- Database ayat pilihan & terjemahan
└── full-launcher-source/          <-- Full source code Gradle launcher siap build
    ├── build.gradle
    ├── settings.gradle
    ├── gradlew / gradlew.bat
    └── app/
```

---

## 4. Panduan Upload ke GitHub

### Opsi A: Jika Anda Sudah Memiliki Repositori Git Lokal
Jika Anda sudah melakukan `git clone` terhadap repositori CleanYourTV / ClearTV Anda:
1. Salin isi dari folder `full-launcher-source/` atau timpa berkas dari folder `changed-files/` ke dalam folder repositori lokal Anda.
2. Buka terminal/PowerShell di folder repositori Anda, lalu jalankan:
   ```shell
   git add .
   git commit -m "feat(launcher): add circular system meters, prayer alignment, and PrayerDream screensaver"
   git push origin main
   ```

### Opsi B: Membuat Repositori Baru dari Folder Ini (Git CLI)
Jika Anda ingin membuat repositori baru di GitHub:
1. Buka folder `full-launcher-source` di terminal.
2. Jalankan perintah berikut:
   ```shell
   cd full-launcher-source
   git init
   git branch -M main
   git add .
   git commit -m "Initial commit: CleanYourTV Launcher with Circular Stats and Prayer Screensaver"
   git remote add origin https://github.com/USERNAME-ANDA/NAMA-REPO-ANDA.git
   git push -u origin main
   ```

### Opsi C: Upload Melalui Web Browser (github.com)
1. Buka repositori Anda di browser melalui [github.com](https://github.com).
2. Klik tombol **Add file** > **Upload files**.
3. Tarik (*drag & drop*) berkas atau folder yang ingin di-update dari folder `changed-files` atau `full-launcher-source`.
4. Tulis pesan commit pada bagian bawah:
   - Judul: `Update Home layout (circular meters) & Prayer screensaver`
   - Klik **Commit changes**.

### Opsi D: Membuat Release APK di GitHub
1. Pada halaman repositori GitHub Anda, klik menu **Releases** di sisi kanan > **Draft a new release**.
2. Masukkan Tag versi, misalnya: `v3.4-circular-stats`.
3. Upload file `app-release.apk` dan `CleanYourTV-system-3.4.zip` yang ada di folder `release-apk/`.
4. Klik **Publish release**.

---

## 5. Kompilasi Ulang (Building from Source)

Jika ingin memodifikasi kembali dan mengompilasi ulang APK:
```shell
cd full-launcher-source
.\gradlew.bat assembleRelease
```
File APK hasil build akan berada di `app/build/outputs/apk/release/app-release.apk`.
