package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val url: String
)

@Composable
fun OpenSourceLicensesView(
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val layoutDirection = LocalLayoutDirection.current
    val uriHandler = LocalUriHandler.current

    val libraries = listOf(
        OpenSourceLibrary(
            "Activity Compose",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/activity"
        ),
        OpenSourceLibrary(
            "AndroidX AppCompat",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/appcompat"
        ),
        OpenSourceLibrary(
            "AndroidX CoordinatorLayout",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/coordinatorlayout"
        ),
        OpenSourceLibrary(
            "AndroidX Core SplashScreen",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/core#core-splashscreen"
        ),
        OpenSourceLibrary(
            "AndroidX ExifInterface",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/exifinterface"
        ),
        OpenSourceLibrary(
            "Coil Compose",
            "Coil Contributors",
            "Apache 2.0",
            "https://github.com/coil-kt/coil"
        ),
        OpenSourceLibrary(
            "Coil GIF",
            "Coil Contributors",
            "Apache 2.0",
            "https://github.com/coil-kt/coil"
        ),
        OpenSourceLibrary(
            "Coil SVG",
            "Coil Contributors",
            "Apache 2.0",
            "https://github.com/coil-kt/coil"
        ),
        OpenSourceLibrary(
            "Coil Video",
            "Coil Contributors",
            "Apache 2.0",
            "https://github.com/coil-kt/coil"
        ),
        OpenSourceLibrary(
            "Compose Material 3",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/compose-material3"
        ),
        OpenSourceLibrary(
            "Compose Material Icons Extended",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/compose-material"
        ),
        OpenSourceLibrary(
            "Compose UI",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/compose-ui"
        ),
        OpenSourceLibrary(
            "Compose UI Graphics",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/compose-ui"
        ),
        OpenSourceLibrary(
            "Compose UI Tooling Preview",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/compose-ui"
        ),
        OpenSourceLibrary(
            "Gson",
            "Google",
            "Apache 2.0",
            "https://github.com/google/gson"
        ),
        OpenSourceLibrary(
            "Lucide",
            "Lucide Contributors",
            "ISC (with MIT portions from Feather)",
            "https://lucide.dev/license"
        ),
        OpenSourceLibrary(
            "Lottie Compose",
            "Airbnb",
            "Apache 2.0",
            "https://github.com/airbnb/lottie-android"
        ),
        OpenSourceLibrary(
            "Backdrop",
            "Kyant",
            "Apache 2.0",
            "https://github.com/Kyant0/AndroidLiquidGlass"
        ),
        OpenSourceLibrary(
            "WebGradients",
            "itmeo",
            "MIT",
            "https://github.com/itmeo/webgradients"
        ),
        OpenSourceLibrary(
            "coolhue",
            "Webkul",
            "MIT",
            "https://github.com/webkul/coolhue"
        ),
        OpenSourceLibrary(
            "1500-color-names",
            "Michael Petrochuk",
            "MIT",
            "https://github.com/PetrochukM/1500-color-names"
        ),
        OpenSourceLibrary(
            "MexBrewer",
            "Pablo Ernesto Azompa",
            "MIT",
            "https://github.com/paezha/MexBrewer"
        ),
        OpenSourceLibrary(
            "ghibli",
            "Ewen Henderson",
            "MIT",
            "https://github.com/ewenme/ghibli"
        ),
        OpenSourceLibrary(
            "hermitage",
            "Eva Dudukina",
            "MIT",
            "https://github.com/dudukina/hermitage"
        ),
        OpenSourceLibrary(
            "dutchmasters",
            "Edwin Thoen",
            "MIT",
            "https://github.com/EdwinTh/dutchmasters"
        ),
        OpenSourceLibrary(
            "ochRe",
            "Holly Kirk et al.",
            "MIT",
            "https://github.com/hollylkirk/ochRe"
        ),
        OpenSourceLibrary(
            "lisa",
            "Tyler Littlefield",
            "MIT",
            "https://github.com/tylerlittlefield/lisa"
        ),
        OpenSourceLibrary(
            "Pigmentpedia",
            "neuralpain",
            "MIT",
            "https://github.com/neuralpain/pigmentpedia"
        ),
        OpenSourceLibrary(
            "vangogh",
            "Cheryl Isabella Lim",
            "MIT",
            "https://github.com/cherylisabella/vangogh"
        ),
        OpenSourceLibrary(
            "dictionary-of-colour-combinations",
            "mattdesl",
            "MIT",
            "https://github.com/mattdesl/dictionary-of-colour-combinations"
        ),
        OpenSourceLibrary(
            "color-names",
            "meodai & contributors",
            "MIT",
            "https://github.com/meodai/color-names"
        ),
        OpenSourceLibrary(
            "Kotlin",
            "JetBrains",
            "Apache 2.0",
            "https://kotlinlang.org/"
        ),
        OpenSourceLibrary(
            "Kotlinx Coroutines Play Services",
            "Kotlin Contributors",
            "Apache 2.0",
            "https://github.com/Kotlin/kotlinx.coroutines"
        ),
        OpenSourceLibrary(
            "Media3 Common",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/media/media3"
        ),
        OpenSourceLibrary(
            "Media3 ExoPlayer",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/media/media3/exoplayer"
        ),
        OpenSourceLibrary(
            "Media3 Transformer",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/media/media3/transformer"
        ),
        OpenSourceLibrary(
            "Media3 UI",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/media/media3"
        ),
        OpenSourceLibrary(
            "CameraX Core",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/camera"
        ),
        OpenSourceLibrary(
            "CameraX Camera2",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/camera"
        ),
        OpenSourceLibrary(
            "CameraX Lifecycle",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/camera"
        ),
        OpenSourceLibrary(
            "CameraX View",
            "Google",
            "Apache 2.0",
            "https://developer.android.com/jetpack/androidx/releases/camera"
        ),
        OpenSourceLibrary(
            "TensorFlow Lite",
            "TensorFlow Authors",
            "Apache 2.0",
            "https://www.tensorflow.org/lite/"
        ),
        OpenSourceLibrary(
            "TensorFlow Lite GPU",
            "TensorFlow Authors",
            "Apache 2.0",
            "https://www.tensorflow.org/lite/"
        ),
        OpenSourceLibrary(
            "TensorFlow Lite GPU API",
            "TensorFlow Authors",
            "Apache 2.0",
            "https://www.tensorflow.org/lite/"
        ),
        OpenSourceLibrary(
            "ML Kit Image Labeling",
            "Google",
            "Apache 2.0",
            "https://developers.google.com/ml-kit/vision/image-labeling"
        ),
        OpenSourceLibrary(
            "ML Kit Face Detection",
            "Google",
            "Apache 2.0",
            "https://developers.google.com/ml-kit/vision/face-detection"
        ),
        OpenSourceLibrary(
            "MNN",
            "Alibaba",
            "Apache 2.0",
            "https://github.com/alibaba/MNN"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, bottom = 24.dp)
        ) {
            UnifiedSubpageTopBarIconButton(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = LocalAppStrings.current.back,
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart),
                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                iconTint = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = strings.openSourceLicenses ?: "Open Source Licenses",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            items(libraries) { lib ->
                OpenSourceLibraryItem(lib) {
                    uriHandler.openUri(lib.url)
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun OpenSourceLibraryItem(
    library: OpenSourceLibrary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = library.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${library.author} - ${library.license}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}
