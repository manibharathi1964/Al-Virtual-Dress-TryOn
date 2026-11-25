# AI Virtual Dress Try-On inference server

This server keeps the FASHN API key off the Android APK and exposes a small application API for the mobile client.

## Requirements

- Node.js 20+
- A FASHN API key with available credits

## Run locally

```bash
cd server
npm install
FASHN_API_KEY=your_key npm start
```

The Android emulator can use the default development endpoint `http://10.0.2.2:8080`.

For a real device, deploy this service to HTTPS and build the APK with:

```bash
./gradlew assembleDebug -PvtoApiBaseUrl=https://your-backend.example.com
```

Do not put the FASHN API key in Android source code, Gradle properties committed to Git, or the APK.

## Endpoint

`POST /v1/tryon`

JSON body:

```json
{
  "model_image": "data:image/jpeg;base64,...",
  "garment_image": "data:image/jpeg;base64,...",
  "category": "auto",
  "mode": "balanced"
}
```

The response contains `image_data_uri` with the generated try-on result.

FASHN supports data-URI inputs and base64 outputs for the try-on API. The provider key is intended for server-side use.
