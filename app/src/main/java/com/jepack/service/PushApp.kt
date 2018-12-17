package com.jepack.service

data class PushApp( var flag:Int = 0,
                    var url:String = "",
                    var pkg:String = "",
                    var md5:String = "",
                    var appCode:Int = 0,
                    var desc:String = "",
                    var title:String = "",
                    var force:Boolean = false)