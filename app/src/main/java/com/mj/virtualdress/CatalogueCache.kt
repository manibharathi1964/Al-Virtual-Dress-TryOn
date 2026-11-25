package com.mj.virtualdress

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object CatalogueCache {
    private const val PREFS = "catalogue_cache"
    private const val KEY = "products"
    fun save(context: Context, products: List<CatalogueProduct>) {
        val array = JSONArray()
        products.forEach { p ->
            array.put(JSONObject().apply {
                put("code", p.code); put("name", p.name); put("category", p.category); put("price", p.price); put("imageUrl", p.imageUrl)
                put("variants", JSONArray().apply {
                    p.variants.forEach { v -> put(JSONObject().apply { put("color", v.color); put("pattern", v.pattern); put("material", v.material); put("sizes", JSONArray(v.sizes)); put("stock", JSONObject(v.stockBySize)) }) }
                })
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
    fun load(context: Context): List<CatalogueProduct> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val p = array.getJSONObject(i); val variants = p.getJSONArray("variants")
                val list = List(variants.length()) { j ->
                    val v = variants.getJSONObject(j); val stock = v.getJSONObject("stock")
                    val stockMap = buildMap { val keys = stock.keys(); while (keys.hasNext()) { val key = keys.next(); put(key, stock.getInt(key)) } }
                    ProductVariant(v.getString("color"), List(v.getJSONArray("sizes").length()) { k -> v.getJSONArray("sizes").getString(k) }, stockMap, v.optString("pattern", "Solid"), v.optString("material", "Standard"))
                }
                CatalogueProduct(p.getString("code"), p.getString("name"), p.getString("category"), p.getInt("price"), p.getString("imageUrl"), list)
            }
        }.getOrDefault(emptyList())
    }
}