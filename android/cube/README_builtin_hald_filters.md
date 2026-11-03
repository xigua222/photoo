# Built-in Hald Filters

Source PNG files live in `cube/`.

Build behavior:
- `preBuild` runs `syncColorCardBuiltinHaldAssets`.
- Every valid Hald PNG is imported into generated app assets.
- The task tries `lossless WebP` first.
- WebP is only used when it is smaller and its decoded RGBA hash matches the source PNG exactly.
- Otherwise the original PNG is packaged.

Generated outputs:
- `app/build/generated/assets/color_cards/color_card_builtin_hald_filters.json`
- `app/build/generated/assets/color_cards/color_card_hald/*`

Runtime:
- The app reads the generated manifest and loads the selected LUT asset on demand.
- Preview colors and `haldLevel` are generated at build time, so entering the filter panel does not decode every LUT file.
