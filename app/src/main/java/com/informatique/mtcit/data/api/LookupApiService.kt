package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.City
import com.informatique.mtcit.data.model.Country
import com.informatique.mtcit.data.model.LookupResponse
import com.informatique.mtcit.data.model.Port
import com.informatique.mtcit.data.model.ShipType
import com.informatique.mtcit.di.module.AppHttpRequests
import com.informatique.mtcit.ui.components.DefaultBusinessIcon
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lookup API Service for fetching dropdown options
 * Currently using MOCK data - will be replaced with real API endpoints
 */
@Singleton
class LookupApiService @Inject constructor(
    client: HttpClient
) : AppHttpRequests(client) {

    /**
     * Get list of ports
     * TODO: Replace with real endpoint when API is ready
     */
    suspend fun getPorts(): Result<LookupResponse<Port>> {
        // Simulate API delay
        delay(500)

        // TODO: Replace with actual API call
        // val result = onGetData("${BASE_URL}/api/lookups/ports")
        // return when (result) {
        //     is AppHttpRequest.AppHttpRequestModel -> {
        //         Result.success(result.response.body<LookupResponse<Port>>())
        //     }
        //     is AppHttpRequest.AppHttpRequestErrorModel -> {
        //         Result.failure(Exception(result.message))
        //     }
        // }

        // Mock implementation
        return Result.success(createMockPortsResponse())
    }

    /**
     * Get list of countries
     * TODO: Replace with real endpoint when API is ready
     */
    suspend fun getCountries(): Result<LookupResponse<Country>> {
        delay(500)

        // TODO: Replace with actual API call
        // val result = onGetData("${BASE_URL}/api/lookups/countries")

        return Result.success(createMockCountriesResponse())
    }

    /**
     * Get list of ship types
     * TODO: Replace with real endpoint when API is ready
     */
    suspend fun getShipTypes(): Result<LookupResponse<ShipType>> {
        delay(500)

        // TODO: Replace with actual API call
        // val result = onGetData("${BASE_URL}/api/lookups/ship-types")

        return Result.success(createMockShipTypesResponse())
    }

    /**
     * Get cities by country
     * TODO: Replace with real endpoint when API is ready
     */
    suspend fun getCitiesByCountry(countryId: String): Result<LookupResponse<City>> {
        delay(500)

        // TODO: Replace with actual API call
        // val result = onGetData("${BASE_URL}/api/lookups/cities?countryId=$countryId")

        return Result.success(createMockCitiesResponse(countryId))
    }

    /**
     * Get list of commercial registrations
     * TODO: Replace with real endpoint when API is ready
     */
    suspend fun getCommercialRegistrations(): Result<LookupResponse<SelectableItem>> {
        delay(500)

        // TODO: Replace with actual API call
        // val result = onGetData("${BASE_URL}/api/lookups/commercial-registrations")

        return Result.success(createMockCommercialRegistrationsResponse())
    }

    /**
     * Get list of person types
     * TODO: Replace with real endpoint when API is ready
     */
    suspend fun getPersonTypes(): Result<LookupResponse<PersonType>> {
        delay(500)

        // TODO: Replace with actual API call
        // val result = onGetData("${BASE_URL}/api/lookups/person-type")

        return Result.success(createMockPersonTypesResponse())
    }

    // ========== MOCK DATA GENERATION ==========
    // Remove these methods when real API is ready

    private fun createMockPortsResponse(): LookupResponse<Port> {
        val ports = listOf(
            Port("1", "ميناء صحار", "Sohar Port", "SOH"),
            Port("2", "ميناء صلالة", "Salalah Port", "SAL"),
            Port("3", "ميناء مسقط", "Muscat Port", "MUS"),
            Port("4", "ميناء الدقم", "Duqm Port", "DUQ"),
            Port("5", "ميناء شناص", "Shinas Port", "SHI")
        )
        return LookupResponse(true, ports)
    }

    private fun createMockCountriesResponse(): LookupResponse<Country> {
        val countries = listOf(
            Country("1", "عُمان", "Oman", "OM"),
            Country("2", "الإمارات العربية المتحدة", "United Arab Emirates", "AE"),
            Country("3", "المملكة العربية السعودية", "Saudi Arabia", "SA"),
            Country("4", "الكويت", "Kuwait", "KW"),
            Country("5", "البحرين", "Bahrain", "BH"),
            Country("6", "قطر", "Qatar", "QA"),
            Country("7", "مصر", "Egypt", "EG"),
            Country("8", "الأردن", "Jordan", "JO")
        )
        return LookupResponse(true, countries)
    }

    private fun createMockShipTypesResponse(): LookupResponse<ShipType> {
        val shipTypes = listOf(
            ShipType("1", "سفينة شحن", "Cargo Ship"),
            ShipType("2", "سفينة ركاب", "Passenger Ship"),
            ShipType("3", "ناقلة نفط", "Oil Tanker"),
            ShipType("4", "يخت", "Yacht"),
            ShipType("5", "قارب صيد", "Fishing Boat"),
            ShipType("6", "قارب نزهة", "Pleasure Boat")
        )
        return LookupResponse(true, shipTypes)
    }

    private fun createMockCitiesResponse(countryId: String): LookupResponse<City> {
        val cities = when (countryId) {
            "1" -> listOf( // Oman
                City("1", "مسقط", "Muscat", "1"),
                City("2", "صلالة", "Salalah", "1"),
                City("3", "صحار", "Sohar", "1"),
                City("4", "نزوى", "Nizwa", "1"),
                City("5", "صور", "Sur", "1")
            )
            "2" -> listOf( // UAE
                City("6", "دبي", "Dubai", "2"),
                City("7", "أبوظبي", "Abu Dhabi", "2"),
                City("8", "الشارقة", "Sharjah", "2")
            )
            else -> listOf()
        }
        return LookupResponse(true, cities)
    }

    private fun createMockCommercialRegistrationsResponse(): LookupResponse<SelectableItem> {
        val data = listOf(
            SelectableItem(
                id = "CR-2024-001",
                title = "شركة النور للتجارة",
                code = "CR-2024-001",
                description = "شركة تجارية متخصصة في استيراد وتصدير\nالمواد الغذائية"
            ),
            SelectableItem(
                id = "CR-2024-002",
                title = "مؤسسة البحر للملاحة",
                code = "CR-2024-002",
                description = "مؤسسة متخصصة في النقل البحري\nوالخدمات اللوجستية"
            ),
            SelectableItem(
                id = "CR-2024-003",
                title = "مؤسسة فلامنجو للملاحة",
                code = "CR-2024-003",
                description = "مؤسسة متخصصة في النقل البحري\nوالخدمات اللوجستية"
            ),
            SelectableItem(
                id = "CR-2024-004",
                title = "شركة الأفق للاستثمار",
                code = "CR-2024-004",
                description = "شركة استثمارية في مجال العقارات"
            )
        )
        return LookupResponse(true, data)
    }

    private fun createMockPersonTypesResponse(): LookupResponse<PersonType> {
        val data = listOf(
            PersonType(
                id = "PT-2024-001",
                title = "فرد",
                code = "PT-2024-001",
                icon = { DefaultBusinessIcon(false) }
            ),
            PersonType(
                id = "PT-2024-002",
                title = "شركة",
                code = "PT-2024-002"
            )
        )
        return LookupResponse(true, data)
    }
}
