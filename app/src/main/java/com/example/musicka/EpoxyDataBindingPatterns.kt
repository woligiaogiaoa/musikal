package com.example.musicka

import com.airbnb.epoxy.EpoxyDataBindingPattern

@EpoxyDataBindingPattern(rClass = R::class, layoutPrefix = "epoxy_layout") //根据这个前缀生成 epoxyModels
private object EpoxyDataBindingPatterns