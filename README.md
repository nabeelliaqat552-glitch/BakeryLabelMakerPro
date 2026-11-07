# Bakery Label Maker Pro — Android (Kotlin + Jetpack Compose)

A native Android app for bakeries, sweet / mithai shops and food packers to create, save and print
MFG / EXP product labels. Works completely offline.

- **Language:** Kotlin 2.0 · **UI:** Jetpack Compose (Material 3) · **Database:** Room (SQLite)
- **Min Android:** 8.0 (API 26) · **Target:** Android 15 (API 35)
- **Package:** `com.bakerylabel.pro` (debug build installs as `com.bakerylabel.pro.debug`, so both can sit side by side)

---

## 1. Build the APK (Android Studio)

1. Install **Android Studio** (Ladybug 2024.2 or newer) from developer.android.com/studio.
2. Unzip this project and choose **File → Open…** → select the `BakeryLabelMakerPro` folder.
3. Wait for **Gradle sync** to finish. The first sync downloads Gradle 8.11.1 and the libraries, so it needs internet once. Accept any prompt to install SDK 35.
4. **Debug APK:** menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
   The file appears at `app/build/outputs/apk/debug/app-debug.apk`.

Command-line alternative (from the project folder): `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).

## 2. Signed release APK

**Easiest — in Android Studio:** **Build → Generate Signed App Bundle / APK… → APK → Create new…** keystore,
fill in passwords, choose **release**, Finish. Output: `app/release/app-release.apk`.

**Or from the command line:**

```bash
# 1. Create your private signing key (keep this file and the passwords safe — you need them for every update)
keytool -genkeypair -v -keystore bakery-release.jks -alias bakery -keyalg RSA -keysize 2048 -validity 10000

# 2. Copy keystore.properties.example to keystore.properties and fill in your passwords
# 3. Build
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

`keystore.properties` and `*.jks` are in `.gitignore` so they are never committed.

## 3. Install on an Android phone

1. Copy the `.apk` to the phone (USB cable, WhatsApp to yourself, Google Drive, or email).
2. Open it from **Files / Downloads**.
3. If Android asks, allow **Install unknown apps** for that app (Files, Chrome or WhatsApp) and tap **Install**.
4. Or with a USB cable and Developer options → USB debugging on: `adb install app-debug.apk`.

---

## 4. Features and where they live

| Feature | Screen / file |
|---|---|
| Dashboard (12 shortcuts, totals, recent labels, frequent products) | `ui/screens/DashboardScreen.kt` |
| 70+ built-in sweets, bakery & other products, unlimited products and categories, search | `data/AppDatabase.kt` (seed), `ui/screens/ProductsScreen.kt`, `CategoriesScreen.kt` |
| Shop profile with logo, auto-filled on new labels | `ui/screens/ShopScreen.kt` |
| Label editor: all fields, show/hide switches, unlimited custom fields, barcode (Code 128), QR | `ui/screens/EditorScreen.kt` (Content tab) |
| MFG defaults to today; EXP by quick duration (1 day … 6 months, custom) or by hand, always editable; optional per-product shelf life only if *you* set it | Editor → Dates tab, `util/Dates.kt` |
| Designer: 7 colour themes + HSV colour picker, 9 fonts, size, weight, alignment, border style/thickness, corner radius, 6 patterns, logo size/position, drag & resize elements, live preview | Editor → Design tab, `ui/components/LabelPreview.kt`, `ColorPicker.kt` |
| Sizes 1×1 … 4×3 in + custom mm/inch; 5 layouts | Editor → Size & Layout tab, `render/LayoutEngine.kt` |
| 27 built-in templates + save your own | `render/Templates.kt`, `ui/screens/TemplatesScreen.kt` |
| Saved labels: edit, rename, duplicate, delete, search, filter by category, auto-save | `ui/screens/SavedLabelsScreen.kt` |
| Bulk Label Maker: multi-select, same or per-product dates, same design or per-product templates, copies, bulk PDF, bulk Bluetooth, save all | `ui/screens/BulkScreen.kt` |
| A4 sticker sheets: auto or custom rows/columns, margins, gaps, cut guides, multi-page preview, mixed products on one sheet | `print/SheetLayout.kt`, `ui/screens/PrintScreen.kt` |
| PDF (exact label-size pages or A4), PNG at 300 DPI (DPI stored in the file), Android printing | `print/Exporter.kt` |
| Bluetooth thermal printers — ESC/POS and TSPL, 58/80 mm/4-inch heads, 203/300 DPI | `print/BluetoothPrinter.kt`, Settings → Printer |
| Backup / restore / move to another phone | `ui/screens/BackupScreen.kt`, `data/Repository.kt` |
| English / Urdu (RTL), light / dark / system theme, date formats, defaults, CSV export, reset | `ui/screens/SettingsScreen.kt`, `res/values-ur/strings.xml` |

**One renderer for everything.** `render/LabelRenderer.kt` draws the on-screen preview, PNG, PDF,
A4 sheets and thermal prints, in real millimetres. What you see is what prints, and nothing is stretched:
labels are placed on A4 at their true size, and the printer path refuses (with a message) rather than
shrinking a label that is wider than the print head.

**Printing at true size.** In Android's print dialog choose *Actual size / 100 % / no scaling*.
Some printer apps default to "fit to page".

## 5. About cloud backup

Backup writes one file containing products, categories, labels, templates, shop profile (with logo) and settings.
**Back up to Google Drive / file** opens Android's file picker, where Google Drive, OneDrive, Dropbox or the phone
itself can be chosen; **Restore** opens the same picker on any phone. This gives backup, restore and phone-to-phone
transfer without an account system inside the app.

The spec also mentions in-app registration/login via Firebase or Supabase. That part is **not** included: it needs
*your* Firebase project (a `google-services.json` file tied to your Google account), and adding the Firebase plugin
without that file stops the project from building. If you want account-based sync later, create a Firebase project,
add the `google-services.json`, and the existing `Repository.exportBackupJson()` / `restoreBackupJson()` functions can
be uploaded to Firebase Storage or Firestore as-is.

## 6. Bluetooth printer tips

1. Pair the printer first in the phone's **Settings → Bluetooth** (PIN is often `0000` or `1234`).
2. In the app: **App Settings → Printer settings → Choose** and pick it.
3. Choose **ESC/POS** for receipt-style printers, **TSPL** for most label printers (TSC, Xprinter label models, Gprinter).
4. Set the head width (58 mm paper = 48 mm printable, 80 mm = 72 mm, 4-inch label printers = 104 mm) and DPI (most are 203).
5. Press **Test print**. Thermal output is black-and-white automatically.

## 7. Test checklist

Before handing the APK to shops, run through this on a real phone:

- [ ] Add, edit, delete a product; add, rename, delete a category; search
- [ ] Edit shop profile and logo; new label picks them up
- [ ] MFG today / past / future; quick expiry; manual expiry; change MFG after a quick expiry
- [ ] Toggle fields on/off; custom field; barcode; QR
- [ ] Change theme, colours, font, border, pattern; drag and resize an element
- [ ] Save, rename, duplicate, delete a label; restart the app — data is still there
- [ ] Bulk: 8+ products, same dates and per-product dates, different templates, 50 copies
- [ ] A4 sheet: auto and custom rows/columns, margins, preview pages, PDF, print
- [ ] PNG export (check it opens at 300 DPI); single-label PDF
- [ ] Bluetooth printer test print (ESC/POS and/or TSPL)
- [ ] Airplane mode: everything except printing to a network printer still works
- [ ] Backup to Drive, clear app data, restore
- [ ] Switch to Urdu and back; dark mode

## 8. Project layout

```
app/src/main/java/com/bakerylabel/pro/
  BakeryApp.kt, MainActivity.kt, AppContainer.kt
  data/      Room entities, DAOs, database + seed data, settings (DataStore), repository + backup
  model/     Label content & design models, colour themes, label sizes
  render/    LabelRenderer (drawing), LayoutEngine (auto layouts), Templates (27 built-in)
  print/     PDF/PNG export & Android printing, A4 sheet layout, Bluetooth ESC/POS + TSPL
  ui/        theme, shared components, screens
  util/      date helpers
app/src/main/res/  icon, splash, themes, English + Urdu strings
tools/gen_strings.py  regenerates both strings.xml files from one table
```
