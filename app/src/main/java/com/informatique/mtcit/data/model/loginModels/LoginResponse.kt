package com.informatique.mtcit.data.model.loginModels

import com.google.gson.annotations.SerializedName

data class LoginResponse(

	@field:SerializedName("result")
	val result: String? = null,

	@field:SerializedName("UserMainData")
	val userMainData: UserMainData? = null,

	@field:SerializedName("IsThereMandatorySurvey")
	val isThereMandatorySurvey: Boolean? = null,

	@field:SerializedName("StaffProfile")
	val staffProfile: Any? = null,

	@field:SerializedName("details")
	val details: String? = null,

	@field:SerializedName("CardProfile")
	val cardProfile: CardProfile? = null
)

data class UserMainData(

	@field:SerializedName("SE_USER_ACCNT_ID")
	val sEUSERACCNTID: String? = null,

	@field:SerializedName("AUserId")
	val aUserId: String? = null,

	@field:SerializedName("YEAR_DESC_AR")
	val yEARDESCAR: String? = null,

	@field:SerializedName("SA_STAFF_MEMBER_ID")
	val sASTAFFMEMBERID: String? = null,

	@field:SerializedName("AS_FACULTY_INFO_ID")
	val aSFACULTYINFOID: String? = null,

	@field:SerializedName("YEAR_DESC_EN")
	val yEARDESCEN: String? = null,

	@field:SerializedName("ED_ACAD_YEAR_ID")
	val eDACADYEARID: String? = null,

	@field:SerializedName("IsStudent")
	val isStudent: Boolean? = null,

	@field:SerializedName("ENT_MAIN_ID")
	val eNTMAINID: String? = null,

	@field:SerializedName("SEM_DESC_AR")
	val sEMDESCAR: String? = null,

	@field:SerializedName("ED_CODE_SEMESTER_ID")
	val eDCODESEMESTERID: String? = null,

	@field:SerializedName("YEAR_CODE")
	val yEARCODE: String? = null,

	@field:SerializedName("SE_ACCOUNT_ID")
	val sEACCOUNTID: String? = null,

	@field:SerializedName("ED_STUD_ID")
	val eDSTUDID: String? = null,

	@field:SerializedName("SE_USER_ID")
	val sEUSERID: String? = null,

	@field:SerializedName("SEM_DESC_EN")
	val sEMDESCEN: String? = null
)

data class CardProfile(

	@field:SerializedName("ENROLL_AR")
	val eNROLLAR: String? = null,

	@field:SerializedName("NATION_DESCR_AR")
	val nATIONDESCRAR: String? = null,

	@field:SerializedName("ENROLL_EN")
	val eNROLLEN: String? = null,

	@field:SerializedName("IDENT_AR")
	val iDENTAR: String? = null,

	@field:SerializedName("AS_CODE_DEGREE_CLASS_ID")
	val aSCODEDEGREECLASSID: String? = null,

	@field:SerializedName("STUD_EMAIL")
	val sTUDEMAIL: String? = null,

	@field:SerializedName("FACULTY_DESCR_EN")
	val fACULTYDESCREN: String? = null,

	@field:SerializedName("FULLFILLED_CH")
	val fULLFILLEDCH: String? = null,

	@field:SerializedName("DEGREE_AR")
	val dEGREEAR: String? = null,

	@field:SerializedName("BIRTH_DATE")
	val bIRTHDATE: String? = null,

	@field:SerializedName("FACULTY_DESCR_AR")
	val fACULTYDESCRAR: String? = null,

	@field:SerializedName("MAJOR_AR")
	val mAJORAR: String? = null,

	@field:SerializedName("SEM_POINT")
	val sEMPOINT: String? = null,

	@field:SerializedName("GRADUATES_FLAG")
	val gRADUATESFLAG: String? = null,

	@field:SerializedName("GENDER_DESCR_AR")
	val gENDERDESCRAR: String? = null,

	@field:SerializedName("FULL_NAME_AR")
	val fULLNAMEAR: String? = null,

	@field:SerializedName("ACAD_ADV_AR")
	val aCADADVAR: String? = null,

	@field:SerializedName("ACCUM_CH_TOT")
	val aCCUMCHTOT: String? = null,

	@field:SerializedName("NATIONAL_NUMBER")
	val nATIONALNUMBER: String? = null,

	@field:SerializedName("DEGREE_EN")
	val dEGREEEN: String? = null,

	@field:SerializedName("STUD_FACULTY_CODE")
	val sTUDFACULTYCODE: String? = null,

	@field:SerializedName("FULL_NAME_EN")
	val fULLNAMEEN: String? = null,

	@field:SerializedName("LEVEL_AR")
	val lEVELAR: String? = null,

	@field:SerializedName("ACAD_ADV_EN")
	val aCADADVEN: String? = null,

	@field:SerializedName("ACCUM_POINT")
	val aCCUMPOINT: String? = null,

	@field:SerializedName("AS_CODE_DEGREE_ID")
	val aSCODEDEGREEID: String? = null,

	@field:SerializedName("IDENT_EN")
	val iDENTEN: String? = null,

	@field:SerializedName("ACCUM_GPA")
	val aCCUMGPA: String? = null,

	@field:SerializedName("SEM_GPA")
	val sEMGPA: String? = null,

	@field:SerializedName("MAJOR_EN")
	val mAJOREN: String? = null,

	@field:SerializedName("SEM_CH")
	val sEMCH: String? = null,

	@field:SerializedName("STUD_MOBNO")
	val sTUDMOBNO: String? = null,

	@field:SerializedName("LEVEL_EN")
	val lEVELEN: String? = null,

	@field:SerializedName("NATION_DESCR_EN")
	val nATIONDESCREN: String? = null,

	@field:SerializedName("ACCUM_CH")
	val aCCUMCH: String? = null
)
