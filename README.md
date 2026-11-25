# AI Virtual Dress Try-On

A professional Android foundation for a commercial AI virtual dressing-room experience.

## Current app foundation
- Clean Material 3 Android UI
- Person photo capture and gallery import
- Garment photo selection
- Product color and size variants
- Try-on workflow with meaningful processing stages
- Catalogue, Favorites, and Compare sections
- Reset workflow
- Professional, simple app icon
- GitHub Actions debug APK build
- APK artifact name: `Al-Virtual-Dress-TryOn.apk`

## Product direction
Person photo -> garment selection/scan -> AI virtual try-on -> realistic result -> variants -> outfit composition -> compare/favorites -> catalogue selection -> save/download/share.

## AI inference boundary
The Android client is prepared for a dedicated generative virtual try-on inference service. A production-quality result requires an actual VTO model/service; the client must not fake this with a simple image overlay.

## Build
GitHub Actions builds the debug APK on pushes to `main` and pull requests. The workflow renames the generated APK to `Al-Virtual-Dress-TryOn.apk`.

## Scope reference
The project scope includes P0 build stability through P9 privacy hardening, with realistic AI generation as the core capability.