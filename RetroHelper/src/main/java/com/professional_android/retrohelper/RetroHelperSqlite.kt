package com.professional_android.retrohelper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import kotlin.collections.ArrayList


class RetroHelperSqlite<T>(private val context: Context, private val modelClass: Class<T>) :
    SQLiteOpenHelper(context, modelClass.name, null, 1) {
    private var db: SQLiteDatabase? = null


    companion object {
        private const val STRING_KEY = "java.lang.String"
        private const val INT_KEY = "int"
        private const val BOOLEAN_KEY = "boolean"
        private const val SQL_STRING_KEY = "TEXT"

        private const val SQL_INT_KEY = "INTEGER"
        private const val JAVA_KEY_WORD = "java"

        private const val INT_COMPONENT_KEY = "java.lang.Integer"
        private const val BOOLEAN_COMPONENT_KEY = "java.lang.Boolean"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        this.db = db
        createTable(modelClass, modelClass.simpleName)
    }

    fun getData(latestItemSize: Int): ArrayList<T> {
        return readData(latestItemSize, modelClass, modelClass.simpleName)

    }

    fun insertData(model: T): Long {
        return insertSlqData(modelClass, model!!, modelClass.simpleName)
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ${modelClass.name}")
        onCreate(db)
    }


    private fun createTable(modelClass: Class<*>, tableName: String) {

        var foreignKey = ""

        var query = "CREATE TABLE " + tableName + " (_id" + modelClass.simpleName + " INTEGER PRIMARY KEY AUTOINCREMENT"

        for (field in modelClass.declaredFields) {
            query += ", "
            field.isAccessible = true
            query += field.name + " "

            if (field.type.toString() == INT_KEY || field.type.name == INT_COMPONENT_KEY) {
                query += SQL_INT_KEY
            } else if (field.type.toString() == BOOLEAN_KEY || field.type.name== BOOLEAN_COMPONENT_KEY) {
                query += SQL_INT_KEY
            } else if (field.type.isArray) {

                when {
                    field.type.componentType.name == INT_COMPONENT_KEY -> {
                        query += SQL_INT_KEY
                    }
                    field.type.componentType.name == BOOLEAN_COMPONENT_KEY -> {
                        query += SQL_INT_KEY
                    }
                    field.type.componentType.name == STRING_KEY -> {
                        query += SQL_STRING_KEY
                    }
                    field.type.componentType.name.startsWith(context.packageName) -> {
                        createTable(field.type.componentType!!, tableName + field.name)
                        foreignKey += " , FOREIGN KEY (${field.name}) REFERENCES ${tableName + field.name}(_id${field.type.componentType.simpleName})"
                        query += SQL_INT_KEY
                    }
                    else -> {
                        throw Exception("${modelClass.name} variable ${field.name} is not supported data type")
                    }
                }
            } else {

                if (field.type.name.startsWith(JAVA_KEY_WORD)) {
                    if (field.type.name == STRING_KEY) {
                        query += SQL_STRING_KEY
                    } else {
                        throw Exception("${modelClass.name} variable ${field.name} is not supported data type")
                    }
                } else if (field.type.name.startsWith(context.packageName)) {
                    createTable(field.type, tableName + field.name)
                    foreignKey += " , FOREIGN KEY (${field.name}) REFERENCES ${tableName + field.name}(_id${field.type.simpleName})"
                    query += SQL_INT_KEY
                } else {
                    throw Exception("${modelClass.name} variable ${field.name} is not supported data type")
                }
            }

        }

        query += foreignKey

        query += ")"

        db!!.execSQL(query)
    }


    private fun insertSlqData(modelClass: Class<*>, model: Any?, tableName: String): Long {
        val db = this.writableDatabase

        val values = ContentValues()

        for (field in modelClass.declaredFields) {
            field.isAccessible = true
            if (field.type.name == INT_KEY  ) {
                values.put(field.name, (field.getInt(model)))

            } else if (field.type.toString() == BOOLEAN_KEY || field.type.name== BOOLEAN_COMPONENT_KEY) {
                if (field.getBoolean(model)) {
                    values.put(field.name, 1)
                } else {
                    values.put(field.name, 0)
                }

            }else if (field.type.name == INT_COMPONENT_KEY){
                values.put(field.name, field.get(model)?.toString()?.toInt())
            } else if (field.type.isArray) {
                if (field.type.componentType.name == INT_COMPONENT_KEY) {
                    val jsonArray = JSONArray()
                    (field.get(model) as Array<*>).forEach { jsonArray.put((it as Int)) }
                    values.put(field.name, jsonArray.toString())
                } else if (field.type.componentType.name == BOOLEAN_COMPONENT_KEY) {
                    val jsonArray = JSONArray()
                    (field.get(model) as Array<*>).forEach {
                        jsonArray.put(
                            if (it as Boolean) 1 else 0
                        )
                    }

                    values.put(field.name, jsonArray.toString())

                } else if (field.type.componentType.name == STRING_KEY) {

                    val jsonArray = JSONArray()
                    (field.get(model) as Array<*>).forEach {
                        jsonArray.put(
                            it as String
                        )
                    }
                    values.put(field.name, jsonArray.toString())

                } else if (field.type.componentType.name.startsWith(context.packageName) || field.type.isArray) {
                    val jsonArray = JSONArray()

                    for (i in (field.get(model) as Array<*>)) {

                        jsonArray.put(insertSlqData(
                            field.type.componentType!!,
                            i,
                            tableName + field.name
                        ))
                    }
                    values.put(field.name, jsonArray.toString())

                } else {
                    throw Exception("${modelClass.name} variable ${field.name} is not supported data type")
                }


            } else if (field.type.name.startsWith("java")) {
                if (field.type.name == STRING_KEY) {
                    values.put(field.name, (field.get(model)?.toString()))
                }

            } else if (field.type.name.startsWith(context.packageName)) {
                val b = insertSlqData(field.type, field.get(model), tableName + field.name)
                values.put(field.name, b.toInt())

            }

        }

        return db.insert(tableName, null, values)


    }

    private fun readData(modelClass: Class<*>, id: Int, tableName: String): Any {

        val query = "SELECT * FROM $tableName WHERE _id${modelClass.simpleName}=$id"
//            val query = "SELECT * FROM " + modelClass.simpleName + " ORDER BY _id${modelClass.simpleName} DESC LIMIT $latestItemSize"


        val db = this.readableDatabase

        val res = db.rawQuery(query, null)

        res.moveToFirst()
        val model = initializeModelInstance(modelClass, tableName, res)
        res.close()
        return model
    }

    private fun readData(
        latestItemSize: Int,
        modelClass: Class<*>,
        tableName: String
    ): ArrayList<T> {
        var selectItem = "_id" + modelClass.simpleName

        for (field in modelClass.declaredFields) {
            selectItem += ", " + field.name
        }

        val query =
            "SELECT $selectItem FROM $tableName"

        val db = this.readableDatabase

        val res = db.rawQuery(query, null)

        val arr = ArrayList<T>(res.count)

        res.moveToFirst()

        do {

            arr.add(initializeModelInstance(modelClass, tableName, res) as T)
        } while (res.moveToNext())


        res.close()
        return arr
    }


    private fun initializeModelInstance(modelClass: Class<*>, tableName: String, res: Cursor): Any {
        var i = 0



        val model = modelClass.newInstance()

        for (field in modelClass.declaredFields) {
            i++

            field.isAccessible = true

            if (field.type.toString() == INT_KEY || field.type.name == INT_COMPONENT_KEY) {
                field.set(model, res.getInt(i))
            } else if (field.type.toString() == BOOLEAN_KEY || field.type.name== BOOLEAN_COMPONENT_KEY) {
                field.set(model, res.getInt(i) == 1)
            } else if (field.type.isArray) {

                if (field.type.componentType.name == INT_COMPONENT_KEY) {
                    val jsonArray = JSONArray(res.getString(i))
                    val array = Array(jsonArray.length()) { item -> jsonArray.getInt(item) }
                    field.set(model, array)
                } else if (field.type.componentType.name == BOOLEAN_COMPONENT_KEY) {
                    val jsonArray = JSONArray(res.getString(i))
                    val array = Array(jsonArray.length()) { item -> jsonArray.getInt(item) == 1 }
                    field.set(model, array)

                } else if (field.type.componentType.name == STRING_KEY) {
                    val jsonArray = JSONArray(res.getString(i))
                    val array =
                        Array<String>(jsonArray.length()) { item -> jsonArray.getString(item) }
                    field.set(model, array)

                } else if (field.type.componentType.name.startsWith(context.packageName) || field.type.isArray) {
                    val jsonArray = JSONArray(res.getString(i))
                    val array = java.lang.reflect.Array.newInstance(
                        field.type.componentType!!,
                        jsonArray.length()
                    )

                    for (index in 0 until jsonArray.length()) {
                        java.lang.reflect.Array.set(
                            array,
                            index,
                            readData(
                                field.type.componentType!!,
                                jsonArray.get(index).toString().toInt(),
                                tableName + field.name
                            )
                        )
                    }

                    field.set(model, array)

                } else {
                    throw Exception("${modelClass.name} variable ${field.name} is not supported data type")
                }

            } else {

                if (field.type.name.startsWith(JAVA_KEY_WORD)) {
                    if (field.type.name == STRING_KEY) {
                        field.set(model, res.getString(i))
                    } else {
                        throw Exception("${modelClass.name} variable ${field.name} is not supported data type")
                    }
                } else if (field.type.name.startsWith(context.packageName)) {
                    field.set(model, readData(field.type, res.getInt(i), tableName + field.name))

                }
            }

            field.isAccessible = false
        }

        return model
    }



}