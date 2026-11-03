
import sys

with open("debug_log.txt", "w") as log:
    log.write("Start\n")
    try:
        import re
        log.write("Imported re\n")
        
        # ... rest of the script ...
        
        # Define the categories and their fields
        categories = {
            "CommonStrings": [
                "undo", "clean", "cancel", "delete", "done", "back", "confirm", "exit", "create", "update", "remove", "send", "share", "comment", "copy", "apply", "stop", "language", "settings", "skip", "trash", "processed", "deleted", "skippedLabel", "savedSpace", "permissionsNeed", "timeSaved", "totalProcessed", "ofGallery", "savedSpaceRatioFormat", "pressAgainToExit", "version", "copyright", "openSourceLicenses", "checkUpdate", "checkingUpdate", "newVersionAvailable", "noUpdateAvailable", "downloading", "downloadFailed"
            ],
            "MediaStrings": [
                "date", "size", "resolution", "filename", "iso", "shutter", "location", "camera", "live", "unknownLocation"
            ],
            "UIStrings": [
                "stats", "emptyStateTitle", "emptyStateSubtitle", "emptyStateButton"
            ],
            "ReviewStrings": [
                "deleteReviewTitle", "deleteReviewSubtitle", "monthly", "monthlyTitle", "monthlyDoneTitle", "monthlyDoneButton", "selectMonth", "months"
            ],
            "FeatureStrings": [
                "similar", "noSimilarPhotosFound", "scanning", "keepSelected", "keepAll", "deleteAll", "selectBest", "similarPhotosDone", "noSimilarPhotos", "similarPhotosSubtitle",
                "smartOrganizeTitle", "noClustersFound", "moveTo", "move", "collapse", "viewAll", "smartOrganizeSubtitle",
                "deepOrganizeTitle", "deepOrganizeScanning", "deepOrganizeComplete", "deepOrganizeScanningDesc", "scanningWait", "scanningDescriptionDetailed",
                "lowQuality", "blur", "underexposed", "overexposed", "closedEyes", "smallFile", "lowResolution", "scanLowQuality", "noLowQualityFound", "lowQualityDone", "solidColor", "lowQualitySubtitle"
            ],
            "CompressStrings": [
                "videoCompressTitle", "videoCompressSubtitle", "videoCompressWarningTitle", "videoCompressWarningBody", "videoCompressTargetLabel", "videoCompressBitrateLow", "videoCompressBitrateBalanced", "videoCompressBitrateHigh", "videoCompressEstimate", "videoCompressTimes", "videoCompressResultTitle", "videoCompressResultSaved", "videoCompressResultCount", "videoCompressDeleteOriginal", "videoCompressKeepOriginal", "videoCompressDeleteConfirmTitle", "videoCompressDeleteConfirmBody", "videoCompressBefore", "videoCompressAfter", "videoCompressRatio", "videoCompressAction", "videoCompressing", "videoCompressSelectHint", "videoCompressError", "videoCompressThresholdLabel",
                "imageCompressTitle", "imageCompressSubtitle", "imageCompressWarningTitle", "imageCompressWarningBody", "imageCompressThresholdLabel", "imageCompressResultTitle", "imageCompressResultSaved", "imageCompressResultCount", "imageCompressDeleteOriginal", "imageCompressKeepOriginal", "imageCompressBefore", "imageCompressAfter", "imageCompressRatio", "imageCompressAction", "imageCompressing", "imageCompressSelectHint", "imageCompressError", "imageCompressOriginalLabel", "imageCompressOriginalSize", "imageCompressSelectAll", "imageCompressSelected"
            ],
            "PrivacyStrings": [
                "privacyTitle", "privacyButton", "privacySection1Title", "privacySection1Content", "privacySection2Title", "privacySection2Content", "privacySection3Title", "privacySection3Content", "privacySection4Title", "privacySection4Content", "privacyPolicy", "termsOfService", "agreeToTerms"
            ],
            "OnboardingStrings": [
                "onboardingWelcomeTitle", "onboardingWelcomeSubtitle", "onboardingLanguageTitle", "onboardingLanguageSubtitle", "onboardingPermissionTitle", "onboardingPermissionSubtitle", "onboardingPermissionButton", "onboardingPermissionGranted", "selectLanguage", "manageFilesPermission", "manageFilesPermissionDesc", "onboardingNext", "onboardingBack", "onboardingStartTitle", "onboardingStartSubtitle", "onboardingStartButton"
            ],
            "FilterStrings": [
                "batchSize", "queueOrder", "orderRandom", "orderTime", "releaseKept", "releaseKeptDesc", "released", "releaseDialogTitle", "releaseDialogContent", "filterTitle", "filterType", "filterFolder", "filterAll", "filterImage", "filterVideo", "filterLive", "viewTrash", "recycleBinTitle", "recycleBinRestore", "recycleBinEmpty", "recycleBinDaysLeftFormat", "filterCompletedTitle", "filterCompletedSubtitle", "reviewAgain", "changeFilter", "filterMonth", "muteVideos", "soundEffects", "haptics", "swipePreferences", "swipeLeft", "swipeRight", "swipeUp", "swipeNone", "swipeSave", "swipeAssignAllToast", "comboAnimation"
            ],
            "SocialStrings": [
                "likes", "viewLikes", "commentPlaceholder", "commentsHeader", "commentUser", "noLikes", "removeLikeTitle", "removeLikeMessage", "removeLike", "avatar", "avatarBoy", "avatarGirl", "commentUserMe", "deleteCommentTitle", "deleteCommentMessage", "commentCopied"
            ],
            "StatsStrings": [
                "holidayNewYear", "holidayValentines", "holidayChildrens", "holidayLaborDay", "holidayNationalDay", "holidaySpringFestival", "holidayDragonBoat", "holidayMidAutumn", "groupPhotosTitle", "shootingStatsTitle", "shootingStatsYear", "shootingStatsMonth", "shootingStatsSummary", "shootingStatsSummaryMonth", "shootingStatsItems", "shootingStatsVideoDuration", "shootingStatsMovies", "shootingStatsShortDrama", "shootingStatsTvDrama", "shootingStatsScreenshots", "shootingStatsSelfies", "shootingStatsMostActiveDay", "shootingStatsVideoTitle", "shootingStatsActiveDayTitle", "shootingStatsLocationTitle", "shootingStatsLocationPlaces", "shootingStatsHeatmapTitle", "shootingStatsHeatmapLess", "shootingStatsHeatmapMore"
            ],
            "FolderStrings": [
                "newFolder", "createNewFolder", "folderName", "undoSuccess", "readyForNextBatch", "continueOrganizing", "currentFolder"
            ]
        }

        # Invert the mapping to find category by field
        field_to_category = {}
        for cat, fields in categories.items():
            for field in fields:
                field_to_category[field] = cat

        # Read files
        localization_kt_path = r"d:\Dl\voicelike\android\app\src\main\java\com\voicelike\app\Localization.kt"
        localization_extra_kt_path = r"d:\Dl\voicelike\android\app\src\main\java\com\voicelike\app\LocalizationExtra.kt"

        log.write(f"Reading {localization_kt_path}\n")
        with open(localization_kt_path, "r", encoding="utf-8") as f:
            localization_kt_content = f.read()

        log.write(f"Reading {localization_extra_kt_path}\n")
        with open(localization_extra_kt_path, "r", encoding="utf-8") as f:
            localization_extra_kt_content = f.read()

        # Parse LocalizedStrings data class to get field types and order
        localized_strings_pattern = re.compile(r"data class LocalizedStrings\((.*?)\)", re.DOTALL)
        match = localized_strings_pattern.search(localization_kt_content)
        if not match:
            log.write("Could not find LocalizedStrings definition\n")
            sys.exit(1)

        fields_def = match.group(1)
        field_lines = fields_def.split("\n")
        field_info = {} # field_name -> type_def (e.g. "String", "List<String>", "String? = null")

        for line in field_lines:
            line = line.strip()
            if not line or line.startswith("//"):
                continue
            # remove trailing comma
            if line.endswith(","):
                line = line[:-1]
            
            parts = line.split(":")
            if len(parts) >= 2:
                name = parts[0].strip().replace("val ", "")
                type_def = ":".join(parts[1:]).strip()
                field_info[name] = type_def
        
        log.write(f"Parsed {len(field_info)} fields\n")

        # Generate LocalizationModels.kt
        models_content = "package com.voicelike.app\n\n"
        for cat, fields in categories.items():
            models_content += f"data class {cat}(\n"
            for i, field in enumerate(fields):
                type_def = field_info.get(field, "String") # Default to String if not found (shouldn't happen)
                comma = "," if i < len(fields) - 1 else ""
                models_content += f"    val {field}: {type_def}{comma}\n"
            models_content += ")\n\n"

        log.write("Writing LocalizationModels.kt\n")
        with open(r"d:\Dl\voicelike\android\app\src\main\java\com\voicelike\app\LocalizationModels.kt", "w", encoding="utf-8") as f:
            f.write(models_content)

        # Generate new LocalizedStrings class
        new_localized_strings_class = "class LocalizedStrings(\n"
        for i, (cat, _) in enumerate(categories.items()):
            comma = "," if i < len(categories) - 1 else ""
            cat_param = cat[0].lower() + cat[1:] # camelCase
            new_localized_strings_class += f"    val {cat_param}: {cat}{comma}\n"
        new_localized_strings_class += ") {\n"

        # Add delegation properties
        for cat, fields in categories.items():
            cat_param = cat[0].lower() + cat[1:]
            for field in fields:
                type_def = field_info.get(field, "String")
                # Handle default values in type def (strip them for property type)
                prop_type = type_def.split("=")[0].strip()
                new_localized_strings_class += f"    val {field}: {prop_type} get() = {cat_param}.{field}\n"
        new_localized_strings_class += "}\n"

        def update_initializations(content):
            new_content = content
            starts = [m.start() for m in re.finditer(r"val Strings\w+\s*=\s*LocalizedStrings\s*\(", content)]
            
            for start_idx in reversed(starts):
                balance = 0
                end_idx = -1
                paren_start = content.find("(", start_idx)
                
                for i in range(paren_start, len(content)):
                    char = content[i]
                    if char == '(':
                        balance += 1
                    elif char == ')':
                        balance -= 1
                        if balance == 0:
                            end_idx = i
                            break
                
                if end_idx == -1:
                    log.write("Error parsing parentheses\n")
                    continue
                    
                args_block = content[paren_start+1:end_idx]
                args_dict = {}
                lines = args_block.split("\n")
                current_key = None
                current_value = ""
                
                for line in lines:
                    stripped = line.strip()
                    if not stripped or stripped.startswith("//"):
                        continue
                    
                    key_match = re.match(r"^(\w+)\s*=\s*(.*)", stripped)
                    if key_match:
                        if current_key:
                            args_dict[current_key] = current_value.strip().rstrip(",")
                        current_key = key_match.group(1)
                        current_value = key_match.group(2)
                    else:
                        if current_key:
                            current_value += "\n" + line
                
                if current_key:
                    args_dict[current_key] = current_value.strip().rstrip(",")
                    
                new_args_block = "\n"
                for cat, fields in categories.items():
                    cat_param = cat[0].lower() + cat[1:]
                    new_args_block += f"    {cat_param} = {cat}(\n"
                    for field in fields:
                        if field in args_dict:
                            val = args_dict[field]
                            if "//" in val and not val.strip().startswith('"'): 
                                pass 
                            new_args_block += f"        {field} = {val},\n"
                    new_args_block += "    ),\n"
                new_args_block += ""
                
                new_content = new_content[:paren_start+1] + new_args_block + new_content[end_idx:]

            return new_content

        # Update Localization.kt
        log.write("Updating Localization.kt\n")
        class_def_pattern = re.compile(r"data class LocalizedStrings\((.*?)\)", re.DOTALL)
        new_localization_kt = class_def_pattern.sub(new_localized_strings_class, localization_kt_content)
        new_localization_kt = update_initializations(new_localization_kt)

        with open(localization_kt_path, "w", encoding="utf-8") as f:
            f.write(new_localization_kt)

        # Update LocalizationExtra.kt
        log.write("Updating LocalizationExtra.kt\n")
        new_localization_extra_kt = update_initializations(localization_extra_kt_content)

        with open(localization_extra_kt_path, "w", encoding="utf-8") as f:
            f.write(new_localization_extra_kt)
            
        log.write("Done\n")
        
    except Exception as e:
        log.write(f"Exception: {str(e)}\n")
        import traceback
        log.write(traceback.format_exc())
