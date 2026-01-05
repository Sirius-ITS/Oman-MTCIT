package com.informatique.mtcit.data.model.requests

import com.informatique.mtcit.business.transactions.TransactionType
import kotlinx.serialization.json.*

/**
 * Converts API request detail response to form data map format
 * Used for resuming transactions from accepted requests
 *
 * Follows app architecture pattern - converts API structure to strategy-compatible format
 */
object RequestDataConverter {

    /**
     * Convert API request detail to formData map for strategy resumption
     *
     * @param response The API response containing request details
     * @param transactionType The transaction type to determine which fields to extract
     * @return Map of field names to values that strategies can process
     */
    fun convertToFormData(
        response: RequestDetailResponse,
        transactionType: TransactionType
    ): MutableMap<String, String> {
        val formData = mutableMapOf<String, String>()

        try {
            val dataObject = response.data.jsonObject

            // Extract request ID and metadata
            dataObject["id"]?.jsonPrimitive?.intOrNull?.let {
                formData["requestId"] = it.toString()
            }

            dataObject["requestSerial"]?.jsonPrimitive?.intOrNull?.let {
                formData["requestSerial"] = it.toString()
            }

            dataObject["requestYear"]?.jsonPrimitive?.intOrNull?.let {
                formData["requestYear"] = it.toString()
            }

            // Extract ship info
            val shipInfo = dataObject["shipInfo"]?.jsonObject
            shipInfo?.let {
                extractShipInfo(it, formData)
            }

            // Extract documents
            val documents = dataObject["documents"]?.jsonArray
            documents?.let {
                extractDocuments(it, formData)
            }

            // Extract insurance info if present
            val insuranceInfo = dataObject["insuranceInfo"]?.jsonArray
            insuranceInfo?.let {
                extractInsuranceInfo(it, formData)
            }

            println("✅ Converted API data to formData with ${formData.size} fields")

        } catch (e: Exception) {
            println("❌ Error converting API data to formData: ${e.message}")
            e.printStackTrace()
        }

        return formData
    }

    /**
     * Extract ship information from API response
     */
    private fun extractShipInfo(shipInfo: JsonObject, formData: MutableMap<String, String>) {
        // Ship info ID
        shipInfo["id"]?.jsonPrimitive?.intOrNull?.let {
            formData["shipInfoId"] = it.toString()
            formData["coreShipsInfoId"] = it.toString() // Alias for payment
        }

        shipInfo["isCurrent"]?.jsonPrimitive?.intOrNull?.let {
            formData["isCurrent"] = it.toString()
        }

        // Extract ship details
        val ship = shipInfo["ship"]?.jsonObject
        ship?.let { extractShipDetails(it, formData) }

        // Extract engines
        val engines = shipInfo["shipInfoEngines"]?.jsonArray
        engines?.let { extractEngines(it, formData) }

        // Extract owners
        val owners = shipInfo["shipInfoOwners"]?.jsonArray
        owners?.let { extractOwners(it, formData) }
    }

    /**
     * Extract ship details (main ship object)
     */
    private fun extractShipDetails(ship: JsonObject, formData: MutableMap<String, String>) {
        // Ship ID
        ship["id"]?.jsonPrimitive?.intOrNull?.let {
            formData["shipId"] = it.toString()
        }

        // IMO Number
        ship["imoNumber"]?.jsonPrimitive?.contentOrNull?.let {
            formData["imoNumber"] = it
            formData["IMO"] = it // Alternative field name
        }

        // Call Sign
        ship["callSign"]?.jsonPrimitive?.contentOrNull?.let {
            formData["callSign"] = it
            formData["CallSign"] = it // Alternative field name
        }

        // MMSI Number
        ship["mmsiNumber"]?.jsonPrimitive?.contentOrNull?.let {
            formData["mmsiNumber"] = it
            formData["MMSI"] = it // Alternative field name
        }

        // Official Number
        ship["officialNumber"]?.jsonPrimitive?.contentOrNull?.let {
            formData["officialNumber"] = it
        }

        // Port of Registry
        val port = ship["portOfRegistry"]?.jsonObject
        port?.let {
            it["id"]?.jsonPrimitive?.contentOrNull?.let { portId ->
                formData["registrationPort"] = portId
                formData["portOfRegistry"] = portId
            }
            it["nameEn"]?.jsonPrimitive?.contentOrNull?.let { portName ->
                formData["registrationPortName"] = portName
            }
        }

        // Marine Activity
        val activity = ship["marineActivity"]?.jsonObject
        activity?.let {
            it["id"]?.jsonPrimitive?.intOrNull?.let { activityId ->
                formData["maritimeactivity"] = activityId.toString()
                formData["marineActivity"] = activityId.toString()
            }
        }

        // Ship Category
        val category = ship["shipCategory"]?.jsonObject
        category?.let {
            it["id"]?.jsonPrimitive?.intOrNull?.let { catId ->
                formData["shipCategory"] = catId.toString()
            }
        }

        // Ship Type
        val shipType = ship["shipType"]?.jsonObject
        shipType?.let {
            it["id"]?.jsonPrimitive?.intOrNull?.let { typeId ->
                formData["unitType"] = typeId.toString()
                formData["shipType"] = typeId.toString()
            }
        }

        // Proof Type
        val proofType = ship["proofType"]?.jsonObject
        proofType?.let {
            it["id"]?.jsonPrimitive?.intOrNull?.let { proofId ->
                formData["proofType"] = proofId.toString()
            }
        }

        // Build Country
        val buildCountry = ship["buildCountry"]?.jsonObject
        buildCountry?.let {
            it["id"]?.jsonPrimitive?.contentOrNull?.let { countryId ->
                formData["buildCountry"] = countryId
            }
        }

        // Build Material
        val buildMaterial = ship["buildMaterial"]?.jsonObject
        buildMaterial?.let {
            it["id"]?.jsonPrimitive?.intOrNull?.let { materialId ->
                formData["buildMaterial"] = materialId.toString()
            }
        }

        // Build Year
        ship["shipBuildYear"]?.jsonPrimitive?.intOrNull?.let {
            formData["shipBuildYear"] = it.toString()
            formData["buildYear"] = it.toString()
        }

        // Build End Date
        ship["buildEndDate"]?.jsonPrimitive?.contentOrNull?.let {
            formData["buildEndDate"] = it
        }

        // Tonnage
        ship["grossTonnage"]?.jsonPrimitive?.intOrNull?.let {
            formData["grossTonnage"] = it.toString()
        }

        ship["netTonnage"]?.jsonPrimitive?.intOrNull?.let {
            formData["netTonnage"] = it.toString()
        }

        ship["deadweightTonnage"]?.jsonPrimitive?.intOrNull?.let {
            formData["deadweightTonnage"] = it.toString()
        }

        ship["maxLoadCapacity"]?.jsonPrimitive?.intOrNull?.let {
            formData["maxLoadCapacity"] = it.toString()
        }

        // Dimensions
        ship["vesselLengthOverall"]?.jsonPrimitive?.intOrNull?.let {
            formData["length"] = it.toString()
            formData["totalLength"] = it.toString()
            formData["vesselLengthOverall"] = it.toString()
        }

        ship["vesselBeam"]?.jsonPrimitive?.intOrNull?.let {
            formData["width"] = it.toString()
            formData["totalWidth"] = it.toString()
            formData["vesselBeam"] = it.toString()
        }

        ship["vesselDraft"]?.jsonPrimitive?.intOrNull?.let {
            formData["draft"] = it.toString()
            formData["vesselDraft"] = it.toString()
        }

        ship["vesselHeight"]?.jsonPrimitive?.intOrNull?.let {
            formData["height"] = it.toString()
            formData["vesselHeight"] = it.toString()
        }

        ship["decksNumber"]?.jsonPrimitive?.intOrNull?.let {
            formData["decksNumber"] = it.toString()
        }

        // Dates
        ship["firstRegistrationDate"]?.jsonPrimitive?.contentOrNull?.let {
            formData["firstRegistrationDate"] = it
        }

        ship["requestSubmissionDate"]?.jsonPrimitive?.contentOrNull?.let {
            formData["requestSubmissionDate"] = it
        }

        // Is Temporary
        ship["isTemp"]?.jsonPrimitive?.intOrNull?.let {
            formData["isTemp"] = it.toString()
        }
    }

    /**
     * Extract engines information
     */
    private fun extractEngines(engines: JsonArray, formData: MutableMap<String, String>) {
        val enginesList = mutableListOf<Map<String, String>>()

        engines.forEachIndexed { index, element ->
            val engineObj = element.jsonObject
            val engineMap = mutableMapOf<String, String>()

            // Engine info ID
            engineObj["id"]?.jsonPrimitive?.intOrNull?.let {
                engineMap["shipInfoEngineId"] = it.toString()
            }

            // Engine details
            val engine = engineObj["engine"]?.jsonObject
            engine?.let {
                it["id"]?.jsonPrimitive?.intOrNull?.let { engineId ->
                    engineMap["engineId"] = engineId.toString()
                }

                it["engineSerialNumber"]?.jsonPrimitive?.contentOrNull?.let { serial ->
                    engineMap["engineSerialNumber"] = serial
                }

                it["enginePower"]?.jsonPrimitive?.intOrNull?.let { power ->
                    engineMap["enginePower"] = power.toString()
                }

                it["cylindersCount"]?.jsonPrimitive?.intOrNull?.let { cylinders ->
                    engineMap["cylindersCount"] = cylinders.toString()
                }

                it["engineModel"]?.jsonPrimitive?.contentOrNull?.let { model ->
                    engineMap["engineModel"] = model
                }

                it["engineManufacturer"]?.jsonPrimitive?.contentOrNull?.let { manufacturer ->
                    engineMap["engineManufacturer"] = manufacturer
                }

                it["engineBuildYear"]?.jsonPrimitive?.contentOrNull?.let { year ->
                    engineMap["engineBuildYear"] = year
                }

                // Engine Type
                val engineType = it["engineType"]?.jsonObject
                engineType?.let { type ->
                    type["id"]?.jsonPrimitive?.intOrNull?.let { typeId ->
                        engineMap["engineType"] = typeId.toString()
                    }
                }

                // Engine Country
                val engineCountry = it["engineCountry"]?.jsonObject
                engineCountry?.let { country ->
                    country["id"]?.jsonPrimitive?.contentOrNull?.let { countryId ->
                        engineMap["engineCountry"] = countryId
                    }
                }

                // Engine Status
                val engineStatus = it["engineStatus"]?.jsonObject
                engineStatus?.let { status ->
                    status["id"]?.jsonPrimitive?.intOrNull?.let { statusId ->
                        engineMap["engineStatus"] = statusId.toString()
                    }
                }
            }

            enginesList.add(engineMap)
        }

        // Store engines as JSON for form processing
        if (enginesList.isNotEmpty()) {
            formData["enginesCount"] = enginesList.size.toString()
            // Store serialized engines data
            formData["engines"] = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.serializer(),
                enginesList
            )
        }
    }

    /**
     * Extract owners information
     */
    private fun extractOwners(owners: JsonArray, formData: MutableMap<String, String>) {
        val ownersList = mutableListOf<Map<String, String>>()

        owners.forEachIndexed { index, element ->
            val ownerObj = element.jsonObject
            val ownerMap = mutableMapOf<String, String>()

            // Owner info ID
            ownerObj["id"]?.jsonPrimitive?.intOrNull?.let {
                ownerMap["shipInfoOwnerId"] = it.toString()
            }

            ownerObj["ownershipPercentage"]?.jsonPrimitive?.intOrNull?.let {
                ownerMap["ownershipPercentage"] = it.toString()
            }

            // Owner details
            val owner = ownerObj["owner"]?.jsonObject
            owner?.let {
                it["id"]?.jsonPrimitive?.intOrNull?.let { ownerId ->
                    ownerMap["ownerId"] = ownerId.toString()
                }

                it["ownerName"]?.jsonPrimitive?.contentOrNull?.let { name ->
                    ownerMap["ownerName"] = name
                }

                it["ownerNameEn"]?.jsonPrimitive?.contentOrNull?.let { nameEn ->
                    ownerMap["ownerNameEn"] = nameEn
                }

                it["ownerCivilId"]?.jsonPrimitive?.contentOrNull?.let { civilId ->
                    ownerMap["ownerCivilId"] = civilId
                }

                it["ownerAddress"]?.jsonPrimitive?.contentOrNull?.let { address ->
                    ownerMap["ownerAddress"] = address
                }

                it["ownerPhone"]?.jsonPrimitive?.contentOrNull?.let { phone ->
                    ownerMap["ownerPhone"] = phone
                }

                it["ownerEmail"]?.jsonPrimitive?.contentOrNull?.let { email ->
                    ownerMap["ownerEmail"] = email
                }

                it["isRepresentative"]?.jsonPrimitive?.intOrNull?.let { isRep ->
                    ownerMap["isRepresentative"] = isRep.toString()
                }
            }

            ownersList.add(ownerMap)
        }

        // Store owners as JSON for form processing
        if (ownersList.isNotEmpty()) {
            formData["ownersCount"] = ownersList.size.toString()
            formData["owners"] = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.serializer(),
                ownersList
            )
        }
    }

    /**
     * Extract documents information
     */
    private fun extractDocuments(documents: JsonArray, formData: MutableMap<String, String>) {
        val documentsList = mutableListOf<Map<String, String>>()

        documents.forEach { element ->
            val docObj = element.jsonObject
            val docMap = mutableMapOf<String, String>()

            docObj["docRefNum"]?.jsonPrimitive?.contentOrNull?.let {
                docMap["docRefNum"] = it
            }

            docObj["fileName"]?.jsonPrimitive?.contentOrNull?.let {
                docMap["fileName"] = it
            }

            docObj["docId"]?.jsonPrimitive?.intOrNull?.let {
                docMap["docId"] = it.toString()
            }

            documentsList.add(docMap)
        }

        if (documentsList.isNotEmpty()) {
            formData["documentsCount"] = documentsList.size.toString()
            formData["documents"] = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.serializer(),
                documentsList
            )
        }
    }

    /**
     * Extract insurance information
     */
    private fun extractInsuranceInfo(insuranceInfo: JsonArray, formData: MutableMap<String, String>) {
        // Insurance info extraction - add if needed
        if (insuranceInfo.isNotEmpty()) {
            formData["hasInsurance"] = "true"
        }
    }

    /**
     * Determine transaction type from API response
     */
    fun determineTransactionType(response: RequestDetailResponse): TransactionType? {
        return try {
            val dataObject = response.data.jsonObject
            val requestType = dataObject["requestType"]?.jsonObject
            val requestTypeId = requestType?.get("id")?.jsonPrimitive?.intOrNull

            requestTypeId?.let { TransactionType.fromTypeId(it) }
        } catch (e: Exception) {
            println("❌ Error determining transaction type: ${e.message}")
            null
        }
    }
}

