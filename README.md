# Scanner Poste

App Android (Kotlin + Jetpack Compose) che legge i Data Matrix delle buste Poste e li raggruppa in aggregati da max 600 codici.
Per ogni codice crea un file scan (PNG del Data Matrix ridisegnato, dritto e simmetrico) in `filesDir/aggregati/<id>/`.

## Aprire e avviare
1. Installa Android Studio (Koala o successivo) e apri questa cartella.
2. Attendi la sincronizzazione Gradle (scarica da solo Gradle 8.7 e l'SDK 34).
3. Collega un telefono Android 8.0+ con debug USB attivo e premi Run. L'emulatore non e' adatto: serve la fotocamera vera.

## Struttura
- `data/Db.kt` – Room: aggregati e codici (cancellazione a cascata)
- `data/DataMatrixImage.kt` – genera il file scan con ZXing
- `AppViewModel.kt` – logica: duplicati, limite 600, eliminazione aggregato + file
- `ui/` – Home, Aggregato, Scanner (CameraX + ML Kit), Viewer
