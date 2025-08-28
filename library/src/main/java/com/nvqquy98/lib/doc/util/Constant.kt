package com.nvqquy98.lib.doc.util

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: Constant
 * Author: Victor
 * Date: 2023/09/28 10:36
 * Description: 
 * -----------------------------------------------------------------
 */

object Constant {
    const val INTENT_DATA_KEY = "INTENT_DATA_KEY"
    const val INTENT_SOURCE_KEY = "INTENT_SOURCE_KEY"
    const val INTENT_TYPE_KEY = "INTENT_TYPE_KEY"
    const val INTENT_ENGINE_KEY = "INTENT_ENGINE_KEY"
    const val INTENT_TITLE = "intent_title"
    const val INTENT_POSITION_KEY = "INTENT_POSITION_KEY"

    /**
     * Office platform online preview limitations:
     * Word and PowerPoint documents must be less than 10 MB, Excel must be less than 5 MB;
     * Supported document formats:
     * Word: docx, dotx
     * Excel: xlsx, xlsb, xls, xlsm
     * PowerPoint: pptx, ppsx, ppt, pps, potx, ppsm
     */
    const val MICROSOFT_URL = "https://view.officeapps.live.com/op/view.aspx?src="

    /**
     * XDOC document preview service supports online PDF viewing
     */
    const val XDOC_VIEW_URL = "https://view.officeapps.live.com/op/view.aspx?src="

    /**
     * Google document preview service, requires VPN
     */
    const val GOOGLE_URL = "https://drive.google.com/viewer/viewer?hl=en&embedded=true&url="
}
