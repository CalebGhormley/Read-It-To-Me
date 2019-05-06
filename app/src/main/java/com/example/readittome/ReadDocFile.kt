package com.example.readittome

import java.io.*
/*
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor

object ReadDocFile {
    @JvmStatic
    fun main(args: Array<String>) {
        var file: File? = null
        var extractor: WordExtractor? = null
        try {

            file = File("c:\\New.doc")
            val fis = FileInputStream(file.absolutePath)
            val document = HWPFDocument(fis)
            extractor = WordExtractor(document)
            val fileData = extractor!!.getParagraphText()
            for (i in fileData.indices) {
                if (fileData[i] != null)
                    println(fileData[i])
            }
        } catch (exep: Exception) {
        }

    }
}
        */