package com.kartverket.functions.datadump

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*

val logger = KtorSimpleLogger("DataDumpRoutes")

fun Route.dataDumpRoutes(
    dataDumpService: DataDumpService
) {
    route("/dump") {
        get {
            logger.info("Received get on /dump")
            val fileName = "data.csv"

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
            )
            val datadump = dataDumpService.getDataDump()
            val datadumpCsv = datadump.toCsv()

            call.respondBytes(
                bytes = datadumpCsv.toByteArray(Charsets.UTF_8),
                contentType = ContentType.Text.CSV.withCharset(Charsets.UTF_8)
            )
        }
    }
}

fun List<DumpRow>.toCsv(): String {
    val headers = mutableSetOf("id", "name", "path")
    for (row in this) {
        headers.addAll(row.metadata.keys)
    }
    val headerList = headers.toList()

    return buildString {
        appendLine(headerList.joinToString(","))

        for (row in this@toCsv) {
            appendLine(headerList.joinToString(",") { column ->
                val value = when (column) {
                    "id" -> row.id.toString()
                    "name" -> row.name
                    "path" -> row.path
                    else -> row.metadata[column] ?: ""
                }
                "\"${value.replace("\"", "\"\"")}\""
            })
        }
    }
}
