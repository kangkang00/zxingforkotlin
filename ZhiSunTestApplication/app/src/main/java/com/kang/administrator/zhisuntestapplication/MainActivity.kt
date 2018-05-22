package com.kang.administrator.zhisuntestapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.experimental.async
import java.net.URL

class MainActivity : AppCompatActivity() {
    var urlstr="http://192.168.1.119/andong/web3/app/supper/100.000.000/qdPart.php?vch3=%E4%B8%8A%E6%B5%B7%E6%B1%BD%E8%BD%A6"

    override fun onCreate(savedInstanceState: Bundle?) {
        var TAG = "kang"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        async{
            var url= URL(urlstr)
            Log.i(TAG,url.toString())
            try {
                var resultText=url.readText()
                Log.i(TAG,resultText)
                var resultstr=Gson().fromJson<Allbuwei>(resultText,Allbuwei::class.java)
                Log.i(TAG,resultstr.toString())
            }catch (e:Exception){
                Log.i(TAG,e.toString())
            }
            runOnUiThread(){
                startActivity(Intent(applicationContext,ZXingActivity::class.java))
            }
        }

    }
    data class Allbuwei(var partList:List<Buwei>,var subPartList:List<BuweiCode>)
    data class Buwei(var orientName:String,var orientList:List<BuweiCode>)
    data class BuweiCode(var code:String,var name:String)
}
