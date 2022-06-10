package com.kreash.devblog.common.di

import com.kreash.devblog.common.nav.SystemNav
import com.kreash.devblog.common.nav.SystemNavImpl

class ObjGraphCommon {

    val systemNav: SystemNav by lazy { SystemNavImpl() }

}