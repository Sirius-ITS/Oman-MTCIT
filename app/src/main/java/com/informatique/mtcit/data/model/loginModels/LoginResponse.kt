package com.informatique.mtcit.data.model.loginModels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(

	@SerialName("result")
	val result: String? = null,

	@SerialName("UserMainData")
	val userMainData: UserMainData? = null,

	@SerialName("IsThereMandatorySurvey")
	val isThereMandatorySurvey: Boolean? = null,

	@SerialName("StaffProfile")
	val staffProfile: String? = null,

	@SerialName("details")
	val details: String? = null,

	@SerialName("CardProfile")
	val cardProfile: CardProfile? = null
)

@Serializable
data class UserMainData(

	@SerialName("SE_USER_ACCNT_ID")
	val sEUSERACCNTID: String? = null,

	@SerialName("AUserId")
	val aUserId: String? = null,

	@SerialName("YEAR_DESC_AR")
	val yEARDESCAR: String? = null,

	@SerialName("SA_STAFF_MEMBER_ID")
	val sASTAFFMEMBERID: String? = null,

	@SerialName("AS_FACULTY_INFO_ID")
	val aSFACULTYINFOID: String? = null,

	@SerialName("YEAR_DESC_EN")
	val yEARDESCEN: String? = null,

	@SerialName("ED_ACAD_YEAR_ID")
	val eDACADYEARID: String? = null,

	@SerialName("IsStudent")
	val isStudent: Boolean? = null,

	@SerialName("ENT_MAIN_ID")
	val eNTMAINID: String? = null,

	@SerialName("SEM_DESC_AR")
	val sEMDESCAR: String? = null,

	@SerialName("ED_CODE_SEMESTER_ID")
	val eDCODESEMESTERID: String? = null,

	@SerialName("YEAR_CODE")
	val yEARCODE: String? = null,

	@SerialName("SE_ACCOUNT_ID")
	val sEACCOUNTID: String? = null,

	@SerialName("ED_STUD_ID")
	val eDSTUDID: String? = null,

	@SerialName("SE_USER_ID")
	val sEUSERID: String? = null,

	@SerialName("SEM_DESC_EN")
	val sEMDESCEN: String? = null
)

@Serializable
data class CardProfile(

	@SerialName("ENROLL_AR")
	val eNROLLAR: String? = null,

	@SerialName("NATION_DESCR_AR")
	val nATIONDESCRAR: String? = null,

	@SerialName("ENROLL_EN")
	val eNROLLEN: String? = null,

	@SerialName("IDENT_AR")
	val iDENTAR: String? = null,

	@SerialName("AS_CODE_DEGREE_CLASS_ID")
	val aSCODEDEGREECLASSID: String? = null,

	@SerialName("STUD_EMAIL")
	val sTUDEMAIL: String? = null,

	@SerialName("FACULTY_DESCR_EN")
	val fACULTYDESCREN: String? = null,

	@SerialName("FULLFILLED_CH")
	val fULLFILLEDCH: String? = null,

	@SerialName("DEGREE_AR")
	val dEGREEAR: String? = null,

	@SerialName("BIRTH_DATE")
	val bIRTHDATE: String? = null,

	@SerialName("FACULTY_DESCR_AR")
	val fACULTYDESCRAR: String? = null,

	@SerialName("MAJOR_AR")
	val mAJORAR: String? = null,

	@SerialName("SEM_POINT")
	val sEMPOINT: String? = null,

	@SerialName("GRADUATES_FLAG")
	val gRADUATESFLAG: String? = null,

	@SerialName("GENDER_DESCR_AR")
	val gENDERDESCRAR: String? = null,

	@SerialName("FULL_NAME_AR")
	val fULLNAMEAR: String? = null,

	@SerialName("ACAD_ADV_AR")
	val aCADADVAR: String? = null,

	@SerialName("ACCUM_CH_TOT")
	val aCCUMCHTOT: String? = null,

	@SerialName("NATIONAL_NUMBER")
	val nATIONALNUMBER: String? = null,

	@SerialName("DEGREE_EN")
	val dEGREEEN: String? = null,

	@SerialName("STUD_FACULTY_CODE")
	val sTUDFACULTYCODE: String? = null,

	@SerialName("FULL_NAME_EN")
	val fULLNAMEEN: String? = null,

	@SerialName("LEVEL_AR")
	val lEVELAR: String? = null,

	@SerialName("ACAD_ADV_EN")
	val aCADADVEN: String? = null,

	@SerialName("ACCUM_POINT")
	val aCCUMPOINT: String? = null,

	@SerialName("AS_CODE_DEGREE_ID")
	val aSCODEDEGREEID: String? = null,

	@SerialName("IDENT_EN")
	val iDENTEN: String? = null,

	@SerialName("ACCUM_GPA")
	val aCCUMGPA: String? = null,

	@SerialName("SEM_GPA")
	val sEMGPA: String? = null,

	@SerialName("MAJOR_EN")
	val mAJOREN: String? = null,

	@SerialName("SEM_CH")
	val sEMCH: String? = null,

	@SerialName("STUD_MOBNO")
	val sTUDMOBNO: String? = null,

	@SerialName("LEVEL_EN")
	val lEVELEN: String? = null,

	@SerialName("NATION_DESCR_EN")
	val nATIONDESCREN: String? = null,

	@SerialName("ACCUM_CH")
	val aCCUMCH: String? = null
)
