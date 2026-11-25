# AI Virtual Dress Try-On — Master Project Scope

Repository: Al-Virtual-Dress-TryOn

This file consolidates the authoritative Final Project Scope and the implementation direction agreed in the working conversation.

## Development rule

Build phase-by-phase. Each phase must be implemented and committed before moving to the next. The final APK is produced only after P0–P9 are completed and the final build is verified.

## P0 — Build stability
Reliable Android build, CI, packaging, baseline app.

Status: COMPLETE on the professional foundation branch.

## P1 — Core try-on
Person + garment pipeline and realistic AI generation.

Implemented in this phase:
- Android person and garment image payloads.
- Secure server-side AI boundary.
- FASHN Try-On Max integration through a backend.
- Real provider polling handled server-side.
- Generated result returned to the Android app as a base64 image.
- Configurable Android backend URL.
- No provider API key embedded in the APK.

Production dependency:
- Deploy the server with a real FASHN API key and configure the APK backend URL.

## P2 — Input quality
Person detection, garment isolation, pose/body checks and photo quality validation.

Status: IN PROGRESS.

Implemented in the current P2 work:
- On-device photo quality checks for resolution, lighting and sharpness.
- ML Kit accurate pose detection for static person photos.
- ML Kit selfie segmentation for foreground/person isolation signals.
- Person photo validation is executed before AI try-on generation.
- Garment image resolution/aspect validation is executed before AI try-on generation.
- Validation feedback is shown directly in the Try-On screen.

Remaining P2 work:
- Production-grade garment isolation/segmentation.
- Automatic garment category detection.
- Automatic garment color detection.
- Stronger full-body framing/pose rules.

## P3 — Result quality
Identity preservation, folds, shadows, proportions and resolution tuning.

## P4 — Store layer
Catalogue, product data, variants and stock.

## P5 — Customer workflow
Favorites, compare, selected items and temporary sessions.

## P6 — Store workflow
Barcode/QR access, Staff Mode and fast store-floor flow.

## P7 — Convenience
Save, download, share, QR delivery and meaningful progress UI.

## P8 — Premium AI
Regeneration controls and AI Style Assistant.

## P9 — Privacy hardening
Consent, retention, secure transfer and deletion.

## Product quality target

The final application is intended to be a professional AI Fashion Assistant + Virtual Try-On + Store Catalogue platform. Try-on realism, fast workflow, simple UI, store integration and privacy remain the five highest-priority pillars.

The authoritative scope requires:
Camera/Gallery → Person Detection & Quality Check → Dress Detection/Product Scan → AI Virtual Try-On → Realistic Result → Variant Selection → Outfit Composition → Favorites/Compare → Save/Download/Share/QR → Store Purchase.
