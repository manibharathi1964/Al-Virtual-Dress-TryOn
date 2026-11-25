# AI Virtual Dress Try-On

A professional Android foundation for a clothing-store virtual try-on experience.

## Current foundation
- Person photo selection from gallery
- Person photo capture with camera
- Garment photo selection
- Modern Material 3 UI
- Try-on workflow state
- Clear integration point for a real AI inference service
- Reset and save workflow placeholders

## Product direction
Person photo -> garment capture/selection -> AI virtual try-on -> realistic result -> comparison/favorites/catalogue/share.

## Important
A realistic generative virtual try-on requires a dedicated inference model/service. This first commit deliberately establishes the Android client foundation and does not pretend that a simple image overlay is equivalent to production AI generation.
