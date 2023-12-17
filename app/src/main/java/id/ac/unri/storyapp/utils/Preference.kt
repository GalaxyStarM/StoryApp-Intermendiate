package id.ac.unri.storyapp.utils

import android.content.Context
import android.content.SharedPreferences

object Preference {

    fun initPref(context: Context, name: String): SharedPreferences {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    private fun editPref(context: Context, name: String): SharedPreferences.Editor {
        val sharedPref = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sharedPref.edit()
    }

    fun saveToken(token:String, context: Context) {
        val editor = editPref(context, "onRegister")
        editor.putString("token", token)
        editor.apply()
    }

    fun logOut(context: Context) {
        val editor = editPref(context, "onRegister")
        editor.remove("token")
        editor.remove("status")
        editor.apply()
    }
}