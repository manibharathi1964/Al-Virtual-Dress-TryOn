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
The Android client now defaults to a free Hugging Face ZeroGPU bridge backed by an open virtual try-on model, so the APK can perform real AI try-on without a FASHN API key or paid API account. The free hosted service can cold-start, queue, or become temporarily unavailable. For commercial deployment, replace it with a model/provider whose license explicitly permits commercial use; the free CatVTON/IDM-VTON family used by public demos is generally non-commercial.

## Build
GitHub Actions builds the debug APK on pushes to `main` and pull requests. The workflow renames the generated APK to `Al-Virtual-Dress-TryOn.apk`.

## Scope reference
The project scope includes P0 build stability through P9 privacy hardening, with realistic AI generation as the core capability.\n\n## Free AI mode\n- Default APK endpoint: Hugging Face public ZeroGPU try-on bridge.\n- No FASHN API key is required.\n- The app sends the person and garment images directly to the free hosted inference service and receives the generated result.\n- This mode is intended for personal/testing use; hosted model availability and quotas are not guaranteed.\n