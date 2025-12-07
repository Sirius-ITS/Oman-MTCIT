//package com.informatique.mtcit.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardActions
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Search
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalSoftwareKeyboardController
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.informatique.mtcit.R
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//
//data class Ship(
//    val id: String,
//    val name: String,
//    val type: String,
//    val imoNumber: String,
//    val callSign: String,
//    val maritimeId: String,
//    val registrationPort: String,
//    val maritimeActivity: String
//)
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ShipDimensionsChangeScreen(navController: NavController) {
//    val extraColors = LocalExtraColors.current
//    var searchQuery by remember { mutableStateOf("") }
//    val keyboardController = LocalSoftwareKeyboardController.current
//
//    // بيانات السفن التجريبية
//    val ships = remember {
//        listOf(
//            Ship(
//                id = "1",
//                name = "الزايدة البحرية",
//                type = "نوع الوحدة البحرية",
//                imoNumber = "9900001",
//                callSign = "A9BC2",
//                maritimeId = "470123456",
//                registrationPort = "صحار",
//                maritimeActivity = "صيد"
//            ),
//            Ship(
//                id = "2",
//                name = "الزايدة البحرية",
//                type = "نوع الوحدة البحرية",
//                imoNumber = "9900001",
//                callSign = "A9BC2",
//                maritimeId = "470123456",
//                registrationPort = "صحار",
//                maritimeActivity = "صيد"
//            ),
//            Ship(
//                id = "3",
//                name = "الزايدة البحرية",
//                type = "نوع الوحدة البحرية",
//                imoNumber = "9900001",
//                callSign = "A9BC2",
//                maritimeId = "470123456",
//                registrationPort = "صحار",
//                maritimeActivity = "صيد"
//        )
//        )
//    }
//
//    // تصفية السفن حسب البحث
//    val filteredShips = remember(searchQuery, ships) {
//        if (searchQuery.isBlank()) {
//            ships
//        } else {
//            ships.filter {
//                it.name.contains(searchQuery, ignoreCase = true) ||
//                        it.imoNumber.contains(searchQuery, ignoreCase = true) ||
//                        it.callSign.contains(searchQuery, ignoreCase = true)
//            }
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Column {
//                        Text(
//                            text = "طلب تغيير أبعاد السفينة أو الوحدة البحرية",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Text(
//                            text = "الخطوة 1 من 5",
//                            fontSize = 12.sp,
//                            color = Color.Gray
//                        )
//                    }
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController.navigateUp() }) {
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                            contentDescription = "رجوع",
//                            tint = extraColors.blue2
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = extraColors.background
//                )
//            )
//        },
//        containerColor = extraColors.background
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//        ) {
//            // قسم العنوان والبحث
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//            ) {
//                Text(
//                    text = "السفن المملوكة",
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.Black,
//                    textAlign = TextAlign.End,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Text(
//                    text = "اختر السفينة من بين المملوكة و المسجلة، أو ابحث باستخدام الرقم التعريفي",
//                    fontSize = 14.sp,
//                    color = Color.Gray,
//                    textAlign = TextAlign.End,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Search TextField
//                OutlinedTextField(
//                    mortgageValue = searchQuery,
//                    onValueChange = { searchQuery = it },
//                    modifier = Modifier.fillMaxWidth(),
//                    placeholder = {
//                        Text(
//                            text = "ادخل الرقم التعريفي",
//                            textAlign = TextAlign.End,
//                            modifier = Modifier.fillMaxWidth()
//                        )
//                    },
//                    leadingIcon = {
//                        Icon(
//                            imageVector = Icons.Default.Search,
//                            contentDescription = "بحث",
//                            tint = Color.Gray
//                        )
//                    },
//                    shape = RoundedCornerShape(12.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedContainerColor = Color.White,
//                        unfocusedContainerColor = Color.White,
//                        focusedBorderColor = extraColors.blue1,
//                        unfocusedBorderColor = Color.LightGray
//                    ),
//                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
//                    keyboardActions = KeyboardActions(
//                        onSearch = {
//                            keyboardController?.hide()
//                        }
//                    ),
//                    singleLine = true
//                )
//            }
//
//            // قائمة السفن
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                items(filteredShips) { ship ->
//                    ShipCard(ship = ship)
//                }
//
//                if (filteredShips.isEmpty()) {
//                    item {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(32.dp),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = "لا توجد نتائج",
//                                fontSize = 16.sp,
//                                color = Color.Gray
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun ShipCard(ship: Ship) {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        elevation = CardDefaults.cardElevation(2.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            // Header with icon and title
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                // Radio button placeholder
//                RadioButton(
//                    selected = false,
//                    onClick = { /* Handle selection */ },
//                    colors = RadioButtonDefaults.colors(
//                        selectedColor = extraColors.blue1,
//                        unselectedColor = Color.LightGray
//                    )
//                )
//
//                Column(
//                    horizontalAlignment = Alignment.End,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text(
//                        text = ship.name,
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black
//                    )
//                    Text(
//                        text = ship.type,
//                        fontSize = 14.sp,
//                        color = Color.Gray
//                    )
//                }
//
//                Spacer(modifier = Modifier.width(12.dp))
//
//                // Ship Icon
//                Box(
//                    modifier = Modifier
//                        .size(48.dp)
//                        .background(
//                            color = Color(0xFFF5F5F5),
//                            shape = RoundedCornerShape(12.dp)
//                        ),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.ic_launcher_foreground),
//                        contentDescription = null,
//                        tint = extraColors.blue1,
//                        modifier = Modifier.size(32.dp)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Ship Details
//            ShipDetailRow(label = "رقم IMO", mortgageValue = ship.imoNumber)
//            Spacer(modifier = Modifier.height(8.dp))
//            ShipDetailRow(label = "رمز النداء", mortgageValue = ship.callSign)
//            Spacer(modifier = Modifier.height(8.dp))
//            ShipDetailRow(label = "رقم الهوية البحرية", mortgageValue = ship.maritimeId)
//            Spacer(modifier = Modifier.height(8.dp))
//            ShipDetailRow(label = "ميناء التسجيل", mortgageValue = ship.registrationPort)
//            Spacer(modifier = Modifier.height(8.dp))
//            ShipDetailRow(label = "النشاط البحري", mortgageValue = ship.maritimeActivity)
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // View All Data Button
//            OutlinedButton(
//                onClick = { /* View all details */ },
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp),
//                colors = ButtonDefaults.outlinedButtonColors(
//                    contentColor = extraColors.blue1
//                ),
//                border = ButtonDefaults.outlinedButtonBorder.copy(
//                    width = 1.dp
//                )
//            ) {
//                Text(
//                    text = "عرض جميع البيانات",
//                    fontSize = 14.sp,
//                    modifier = Modifier.padding(vertical = 4.dp)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun ShipDetailRow(label: String, mortgageValue: String) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = mortgageValue,
//            fontSize = 14.sp,
//            color = Color.Black,
//            fontWeight = FontWeight.Medium
//        )
//
//        Text(
//            text = label,
//            fontSize = 14.sp,
//            color = Color.Gray
//        )
//    }
//}

//package com.informatique.mtcit.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardActions
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material.icons.filled.Search
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalSoftwareKeyboardController
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.informatique.mtcit.R
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//import kotlinx.coroutines.launch
//
//data class Ship(
//    val id: String,
//    val name: String,
//    val type: String,
//    val imoNumber: String,
//    val callSign: String,
//    val maritimeId: String,
//    val registrationPort: String,
//    val maritimeActivity: String
//)
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ShipDimensionsChangeScreen(navController: NavController) {
//    val extraColors = LocalExtraColors.current
//    var searchQuery by remember { mutableStateOf("") }
//    val keyboardController = LocalSoftwareKeyboardController.current
//    val sheetState = rememberModalBottomSheetState()
//    val scope = rememberCoroutineScope()
//    var showBottomSheet by remember { mutableStateOf(false) }
//    var selectedShipForDetails by remember { mutableStateOf<Ship?>(null) }
//    val selectedShips = remember { mutableStateListOf<String>() }
//
//    // بيانات السفن التجريبية
//    val ships = remember {
//        listOf(
//            Ship(
//                id = "1",
//                name = "الزايدة البحرية",
//                type = "نوع الوحدة البحرية",
//                imoNumber = "9900001",
//                callSign = "A9BC2",
//                maritimeId = "470123456",
//                registrationPort = "صحار",
//                maritimeActivity = "صيد"
//            ),
//            Ship(
//                id = "2",
//                name = "الزايدة البحرية",
//                type = "نوع الوحدة البحرية",
//                imoNumber = "9900001",
//                callSign = "A9BC2",
//                maritimeId = "470123456",
//                registrationPort = "صحار",
//                maritimeActivity = "صيد"
//            ),
//            Ship(
//                id = "3",
//                name = "الزايدة البحرية",
//                type = "نوع الوحدة البحرية",
//                imoNumber = "9900001",
//                callSign = "A9BC2",
//                maritimeId = "470123456",
//                registrationPort= "صحار",
//            maritimeActivity = "صيد"
//        )
//        )
//    }
//
//    // تصفية السفن حسب البحث
//    val filteredShips = remember(searchQuery, ships) {
//        if (searchQuery.isBlank()) {
//            ships
//        } else {
//            ships.filter {
//                it.name.contains(searchQuery, ignoreCase = true) ||
//                        it.imoNumber.contains(searchQuery, ignoreCase = true) ||
//                        it.callSign.contains(searchQuery, ignoreCase = true)
//            }
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Column {
//                        Text(
//                            text = "طلب تغيير أبعاد السفينة أو الوحدة البحرية",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Text(
//                            text = "الخطوة 1 من 5",
//                            fontSize = 12.sp,
//                            color = Color.Gray
//                        )
//                    }
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController.navigateUp() }) {
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                            contentDescription = "رجوع",
//                            tint = extraColors.blue2
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = extraColors.background
//                )
//            )
//        },
//        containerColor = extraColors.background
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//        ) {
//            // قسم العنوان والبحث
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//            ) {
//                Text(
//                    text = "السفن المملوكة",
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.Black,
//                    textAlign = TextAlign.End,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Text(
//                    text = "اختر السفينة من بين المملوكة و المسجلة، أو ابحث باستخدام الرقم التعريفي",
//                    fontSize = 14.sp,
//                    color = Color.Gray,
//                    textAlign = TextAlign.End,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Search TextField
//                OutlinedTextField(
//                    mortgageValue = searchQuery,
//                    onValueChange = { searchQuery = it },
//                    modifier = Modifier.fillMaxWidth(),
//                    placeholder = {
//                        Text(
//                            text = "ادخل الرقم التعريفي",
//                            textAlign = TextAlign.End,
//                            modifier = Modifier.fillMaxWidth()
//                        )
//                    },
//                    leadingIcon = {
//                        Icon(
//                            imageVector = Icons.Default.Search,
//                            contentDescription = "بحث",
//                            tint = Color.Gray
//                        )
//                    },
//                    shape = RoundedCornerShape(12.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedContainerColor = Color.White,
//                        unfocusedContainerColor = Color.White,
//                        focusedBorderColor = extraColors.blue1,
//                        unfocusedBorderColor = Color.LightGray
//                    ),
//                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
//                    keyboardActions = KeyboardActions(
//                        onSearch = {
//                            keyboardController?.hide()
//                        }
//                    ),
//                    singleLine = true
//                )
//            }
//
//            // قائمة السفن
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                items(filteredShips) { ship ->
//                    ShipCard(
//                        ship = ship,
//                        isSelected = selectedShips.contains(ship.id),
//                        onCardClick = {
//                            if (selectedShips.contains(ship.id)) {
//                                selectedShips.remove(ship.id)
//                            } else {
//                                selectedShips.add(ship.id)
//                            }
//                        },
//                        onShowDetails = {
//                            selectedShipForDetails = ship
//                            showBottomSheet = true
//                        }
//                    )
//                }
//
//                if (filteredShips.isEmpty()) {
//                    item {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(32.dp),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = "لا توجد نتائج",
//                                fontSize = 16.sp,
//                                color = Color.Gray
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    // Bottom Sheet للتفاصيل
//    if (showBottomSheet && selectedShipForDetails != null) {
//        ModalBottomSheet(
//            onDismissRequest = {
//                showBottomSheet = false
//                selectedShipForDetails = null
//            },
//            sheetState = sheetState,
//            containerColor = Color.White,
//            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
//        ) {
//            ShipDetailsBottomSheet(
//                ship = selectedShipForDetails!!,
//                onClose = {
//                    scope.launch {
//                        sheetState.hide()
//                        showBottomSheet = false
//                        selectedShipForDetails = null
//                    }
//                }
//            )
//        }
//    }
//}
//
//@Composable
//fun ShipCard(
//    ship: Ship,
//    isSelected: Boolean,
//    onCardClick: () -> Unit,
//    onShowDetails: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .then(
//                if (isSelected) {
//                    Modifier.border(
//                        width = 2.dp,
//                        color = extraColors.blue1,
//                        shape = RoundedCornerShape(16.dp)
//                    )
//                } else {
//                    Modifier
//                }
//            ),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isSelected) extraColors.blue1.copy(alpha = 0.05f) else Color.White
//        ),
//        elevation = CardDefaults.cardElevation(2.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            // Header with icon and title
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                // Checkbox للاختيار
//                Checkbox(
//                    checked = isSelected,
//                    onCheckedChange = { onCardClick() },
//                    colors = CheckboxDefaults.colors(
//                        checkedColor = extraColors.blue1,
//                        uncheckedColor = Color.LightGray
//                    )
//                )
//
//                Column(
//                    horizontalAlignment = Alignment.End,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text(
//                        text = ship.name,
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black
//                    )
//                    Text(
//                        text = ship.type,
//                        fontSize = 14.sp,
//                        color = Color.Gray
//                    )
//                }
//
//                Spacer(modifier = Modifier.width(12.dp))
//
//                // Ship Icon
//                Box(
//                    modifier = Modifier
//                        .size(48.dp)
//                        .background(
//                            color = Color(0xFFF5F5F5),
//                            shape = RoundedCornerShape(12.dp)
//                        ),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.ic_launcher_foreground),
//                        contentDescription = null,
//                        tint = extraColors.blue1,
//                        modifier = Modifier.size(32.dp)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Ship Details
//            ShipDetailRow(label = "رقم IMO", mortgageValue = ship.imoNumber)
//            Spacer(modifier = Modifier.height(8.dp))
//            ShipDetailRow(label = "رمز النداء", mortgageValue = ship.callSign)
//            Spacer(modifier = Modifier.height(8.dp))
//            ShipDetailRow(label = "رقم الهوية البحرية", mortgageValue = ship.maritimeId)
//            Spacer(modifier = Modifier.height(8.dp))
//            ShipDetailRow(label = "ميناء التسجيل", mortgageValue = ship.registrationPort)
//            Spacer(modifier = Modifier.height(8.dp))
//            ShipDetailRow(label = "النشاط البحري", mortgageValue = ship.maritimeActivity)
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // View All Data Button
//            OutlinedButton(
//                onClick = onShowDetails,
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp),
//                colors = ButtonDefaults.outlinedButtonColors(
//                    contentColor = extraColors.blue1
//                ),
//                border = ButtonDefaults.outlinedButtonBorder.copy(
//                    width = 1.dp
//                )
//            ) {
//                Text(
//                    text = "عرض جميع البيانات",
//                    fontSize = 14.sp,
//                    modifier = Modifier.padding(vertical = 4.dp)
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ShipDetailsBottomSheet(
//    ship: Ship,
//    onClose: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(bottom = 24.dp)
//    ) {
//        // Header
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 24.dp, vertical = 16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(onClick = onClose) {
//                Icon(
//                    imageVector = Icons.Default.Close,
//                    contentDescription = "إغلاق",
//                    tint = Color.Gray
//                )
//            }
//
//            Text(
//                text = "تفاصيل السفينة الكاملة",
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.Black
//            )
//        }
//
//        Divider(color = Color.LightGray.copy(alpha = 0.5f))
//
//        LazyColumn(
//            modifier = Modifier.fillMaxWidth(),
//            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            item {
//                // Ship Icon and Name
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Column(horizontalAlignment = Alignment.End) {
//                        Text(
//                            text = ship.name,
//                            fontSize = 22.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = extraColors.blue1
//                        )
//                        Text(
//                            text = ship.type,
//                            fontSize = 16.sp,
//                            color = Color.Gray
//                        )
//                    }
//
//                    Box(
//                        modifier = Modifier
//                            .size(64.dp)
//                            .background(
//                                color = extraColors.blue1.copy(alpha = 0.1f),
//                                shape = RoundedCornerShape(16.dp)
//                            ),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            painter = painterResource(R.drawable.ic_launcher_foreground),
//                            contentDescription = null,
//                            tint = extraColors.blue1,
//                            modifier = Modifier.size(40.dp)
//                        )
//                    }
//                }
//            }
//
//            item {
//                DetailSection(
//                    title = "المعلومات الأساسية",
//                    details = listOf(
//                        "رقم IMO" to ship.imoNumber,
//                        "رمز النداء" to ship.callSign,
//                        "رقم الهوية البحرية" to ship.maritimeId
//                    )
//                )
//            }
//
//            item {
//                DetailSection(
//                    title = "معلومات التسجيل",
//                    details = listOf(
//                        "ميناء التسجيل" to ship.registrationPort,
//                        "النشاط البحري" to ship.maritimeActivity
//                    )
//                )
//            }
//
//            item {
//                DetailSection(
//                    title = "معلومات إضافية",
//                    details = listOf(
//                        "بلد التسجيل" to "سلطنة عمان",
//                        "سنة الصنع" to "2020",
//                        "نوع المحرك" to "ديزل",
//                        "القوة الحصانية" to "500 HP"
//                    )
//                )
//            }
//
//            item {
//                DetailSection(
//                    title = "الأبعاد",
//                    details = listOf(
//                        "الطول" to "25 متر",
//                        "العرض" to "8 متر",
//                        "العمق" to "4 متر",
//                        "الحمولة الإجمالية" to "150 طن"
//                    )
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun DetailSection(
//    title: String,
//    details: List<Pair<String, String>>
//) {
//    val extraColors = LocalExtraColors.current
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(
//                color = Color(0xFFF8F9FA),
//                shape = RoundedCornerShape(12.dp)
//            )
//            .padding(16.dp)
//    ) {
//        Text(
//            text = title,
//            fontSize = 16.sp,
//            fontWeight = FontWeight.Bold,
//            color = extraColors.blue1,
//            textAlign = TextAlign.End,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        details.forEach { (label, mortgageValue) ->
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 6.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = mortgageValue,
//                    fontSize = 15.sp,
//                    color = Color.Black,
//                    fontWeight = FontWeight.Medium
//                )
//
//                Text(
//                    text = label,
//                    fontSize = 15.sp,
//                    color = Color.Gray
//                )
//            }
//
//            if (details.last() != (label to mortgageValue)) {
//                Divider(
//                    color = Color.LightGray.copy(alpha = 0.3f),
//                    modifier = Modifier.padding(vertical = 4.dp)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun ShipDetailRow(label: String, mortgageValue: String) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = mortgageValue,
//            fontSize = 14.sp,
//            color = Color.Black,
//            fontWeight = FontWeight.Medium
//        )
//
//        Text(
//            text = label,
//            fontSize = 14.sp,
//            color = Color.Gray
//        )
//    }
//}


package com.informatique.mtcit.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.coroutines.launch

data class Ship(
    val id: String,
    val name: String,
    val type: String,
    val imoNumber: String,
    val callSign: String,
    val maritimeId: String,
    val registrationPort: String,
    val maritimeActivity: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipDimensionsChangeScreen(navController: NavController) {
    val extraColors = LocalExtraColors.current
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedShipForDetails by remember { mutableStateOf<Ship?>(null) }
    val selectedShips = remember { mutableStateListOf<String>() }

    val ships = remember {
        listOf(
            Ship("1", "الزايدة البحرية", "سفينة شحن", "9900001", "A9BC2", "470123456", "صحار", "نقل بضائع"),
            Ship("2", "نسيم العرب", "قارب صيد", "9900002", "B8CD3", "470123457", "مسقط", "صيد"),
            Ship("3", "البحر الأزرق", "ناقلة نفط", "9900003", "C7DE4", "470123458", "الدقم", "نقل نفط"),
            Ship("4", "موج السلطان", "سفينة ركاب", "9900004", "D6EF5", "470123459", "صلالة", "نقل ركاب")
        )
    }

    val filteredShips = remember(searchQuery, ships) {
        if (searchQuery.isBlank()) ships
        else ships.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.imoNumber.contains(searchQuery) ||
                    it.callSign.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = extraColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "اختر السفن",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.blue1
                        )
                        AnimatedVisibility(visible = selectedShips.isNotEmpty()) {
                            Text(
                                text = "${selectedShips.size} تم اختيارها",
                                fontSize = 12.sp,
                                color = extraColors.blue1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = extraColors.blue1
                        )
                    }
                },
                actions = {
                    if (selectedShips.isNotEmpty()) {
                        TextButton(onClick = { selectedShips.clear() }) {
                            Text("إلغاء الكل", color = extraColors.blue1)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = extraColors.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(extraColors.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            /** ✅ Search Box (ثابت فوق مش Scrollable) **/
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "ابحث عن سفينة...",
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = extraColors.blue1
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = extraColors.blue1,
                    unfocusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            /** ✅ LazyColumn — فقط للسفن **/
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    filteredShips.forEachIndexed { index, ship ->
                        ModernShipCard(
                            ship = ship,
                            isSelected = selectedShips.contains(ship.id),
                            onCardClick = {
                                if (selectedShips.contains(ship.id)) {
                                    selectedShips.remove(ship.id)
                                } else {
                                    selectedShips.add(ship.id)
                                }
                            },
                            onShowDetails = {
                                selectedShipForDetails = ship
                                showBottomSheet = true
                            },
                            index = index
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                /** ✅ Empty State **/
                if (filteredShips.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "لا توجد نتائج",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet && selectedShipForDetails != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                selectedShipForDetails = null
            },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
            }
        ) {
            ModernShipDetailsBottomSheet(
                ship = selectedShipForDetails!!,
                onClose = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                        selectedShipForDetails = null
                    }
                }
            )
        }
    }
}

@Composable
fun ModernShipCard(
    ship: Ship,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onShowDetails: () -> Unit,
    index: Int
) {
    val extraColors = LocalExtraColors.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val offsetY by animateDpAsState(
        targetValue = if (isSelected) (-4).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "offsetY"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .offset(y = offsetY)
            .clickable(
                onClick = onCardClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Gradient background when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    extraColors.blue1,
                                    Color(0xFF00D4CC)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    // Selection Indicator
                    AnimatedContent(
                        targetState = isSelected,
                        transitionSpec = {
                            (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                        },
                        label = "checkbox"
                    ) { selected ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) extraColors.blue1
                                    else Color.Gray.copy(alpha = 0.1f)
                                )
                                .clickable(onClick = onCardClick),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Ship Info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = ship.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = extraColors.blue1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ship.type,
                            fontSize = 14.sp,
                            color = extraColors.blue2
                        )
                    }

                    // Ship Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        extraColors.blue1.copy(alpha = 0.15f),
                                        extraColors.blue1.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ship_registration),
                            contentDescription = null,
                            tint = extraColors.blue1,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Info Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    InfoChip(
                        label = "IMO",
                        value = ship.imoNumber.takeLast(4),
                        color = extraColors.blue1
                    )
                    InfoChip(
                        label = "رمز النداء",
                        value = ship.callSign,
                        color = extraColors.blue1
                    )
                    InfoChip(
                        label = "الهوية البحرية",
                        value = ship.maritimeId.takeLast(4),
                        color = extraColors.blue1
                    )
                    InfoChip(
                        label = "التسجيل",
                        value = ship.registrationPort,
                        color = extraColors.blue1
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Details Button
                Surface(
                    onClick = onShowDetails,
                    shape = RoundedCornerShape(12.dp),
                    color = extraColors.blue1.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "عرض التفاصيل الكاملة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.blue1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 13.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ModernShipDetailsBottomSheet(ship: Ship, onClose: () -> Unit) {
    val extraColors = LocalExtraColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    tint = Color.Gray
                )
            }

            Text(
                text = "التفاصيل الكاملة",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = extraColors.blue1
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Ship Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        extraColors.blue1,
                                        Color(0xFF00B4CC)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = ship.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = ship.type,
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_launcher_foreground),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                DetailGroup(
                    title = "المعلومات الأساسية",
                    icon = painterResource(id = R.drawable.ic_ship_registration),
                    items = listOf(
                        "رقم IMO" to ship.imoNumber,
                        "رمز النداء" to ship.callSign,
                        "رقم الهوية البحرية" to ship.maritimeId
                    ),
                    color = extraColors.blue1
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailGroup(
                    title = "معلومات التسجيل",
                    icon = painterResource(id = R.drawable.ic_navigation),
                    items = listOf(
                        "ميناء التسجيل" to ship.registrationPort,
                        "النشاط البحري" to ship.maritimeActivity,
                        "بلد التسجيل" to "سلطنة عمان"
                    ),
                    color = extraColors.blue1
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailGroup(
                    title = "المواصفات التقنية",
                    icon = painterResource(id = R.drawable.ic_ship_modification),
                    items = listOf(
                        "سنة الصنع" to "2020",
                        "نوع المحرك" to "ديزل",
                        "القوة الحصانية" to "500 HP"
                    ),
                    color = extraColors.blue1
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailGroup(
                    title = "الأبعاد",
                    icon = painterResource(id = R.drawable.ic_ship_modification),
                    items = listOf(
                        "الطول" to "25 متر",
                        "العرض" to "8 متر",
                        "العمق" to "4 متر",
                        "الحمولة الإجمالية" to "150 طن"
                    ),
                    color = extraColors.blue1
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DetailGroup(
    title: String,
    icon: Painter,
    items: List<Pair<String, String>>,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        val extraColors = LocalExtraColors.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                // Ship Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    extraColors.blue1.copy(alpha = 0.15f),
                                    extraColors.blue1.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = extraColors.blue1,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = value,
                        fontSize = 15.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }
                if (items.last() != (label to value)) {
                    Divider(
                        color = color.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}


//package com.informatique.mtcit.ui.screens
//
//import androidx.compose.animation.*
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.informatique.mtcit.R
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//import kotlinx.coroutines.launch
//
//data class Ship(
//    val id: String,
//    val name: String,
//    val type: String,
//    val imoNumber: String,
//    val callSign: String,
//    val maritimeId: String,
//    val registrationPort: String,
//    val maritimeActivity: String
//)
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ShipDimensionsChangeScreen(navController: NavController) {
//    val extraColors = LocalExtraColors.current
//    var searchQuery by remember { mutableStateOf("") }
//    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//    val scope = rememberCoroutineScope()
//    var showBottomSheet by remember { mutableStateOf(false) }
//    var selectedShipForDetails by remember { mutableStateOf<Ship?>(null) }
//    val selectedShips = remember { mutableStateListOf<String>() }
//
//    val ships = remember {
//        listOf(
//            Ship("1", "الزايدة البحرية", "سفينة شحن", "9900001", "A9BC2", "470123456", "صحار", "نقل بضائع"),
//            Ship("2", "نسيم العرب", "قارب صيد", "9900002", "B8CD3", "470123457", "مسقط", "صيد"),
//            Ship("3", "البحر الأزرق", "ناقلة نفط", "9900003", "C7DE4", "470123458", "الدقم", "نقل نفط"),
//            Ship("4", "موج السلطان", "سفينة ركاب", "9900004", "D6EF5", "470123459", "صلالة", "نقل ركاب")
//        )
//    }
//
//    val filteredShips = remember(searchQuery, ships) {
//        if (searchQuery.isBlank()) ships
//        else ships.filter {
//            it.name.contains(searchQuery, ignoreCase = true) ||
//                    it.imoNumber.contains(searchQuery) ||
//                    it.callSign.contains(searchQuery, ignoreCase = true)
//        }
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        // Gradient Background
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(200.dp)
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(
//                            extraColors.blue1,
//                            extraColors.background
//                        )
//                    )
//                )
//        )
//
//        Column(modifier = Modifier.fillMaxSize()) {
//            // Custom Top Bar
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 16.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                AnimatedVisibility(visible = selectedShips.isNotEmpty()) {
//                    TextButton(onClick = { selectedShips.clear() }) {
//                        Text("إلغاء الكل", color = Color.White, fontSize = 14.sp)
//                    }
//                }
//
//                Column(horizontalAlignment = Alignment.End) {
//                    Text(
//                        text = "اختر السفن المملوكة",
//                        fontSize = 20.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.White
//                    )
//                    AnimatedVisibility(visible = selectedShips.isNotEmpty()) {
//                        Text(
//                            text = "${selectedShips.size} سفينة محددة",
//                            fontSize = 13.sp,
//                            color = Color.White.copy(alpha = 0.9f)
//                        )
//                    }
//                }
//
//                IconButton(
//                    onClick = { navController.navigateUp() },
//                    modifier = Modifier
//                        .size(40.dp)
//                        .clip(CircleShape)
//                        .background(Color.White.copy(alpha = 0.2f))
//                ) {
//                    Icon(
//                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                        contentDescription = "رجوع",
//                        tint = Color.White
//                    )
//                }
//            }
//
//            // Search Card
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp),
//                shape = RoundedCornerShape(20.dp),
//                colors = CardDefaults.cardColors(containerColor = Color.White),
//                elevation = CardDefaults.cardElevation(8.dp)
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    if (searchQuery.isNotEmpty()) {
//                        IconButton(
//                            onClick = { searchQuery = "" },
//                            modifier = Modifier.size(24.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Close,
//                                contentDescription = "مسح",
//                                tint = Color.Gray,
//                                modifier = Modifier.size(20.dp)
//                            )
//                        }
//                    }
//
//                    TextField(
//                        mortgageValue = searchQuery,
//                        onValueChange = { searchQuery = it },
//                        modifier = Modifier.weight(1f),
//                        placeholder = {
//                            Text(
//                                text = "ابحث بالاسم أو الرقم...",
//                                color = Color.Gray,
//                                textAlign = TextAlign.End,
//                                modifier = Modifier.fillMaxWidth()
//                            )
//                        },
//                        colors = TextFieldDefaults.colors(
//                            focusedContainerColor = Color.Transparent,
//                            unfocusedContainerColor = Color.Transparent,
//                            focusedIndicatorColor = Color.Transparent,
//                            unfocusedIndicatorColor = Color.Transparent
//                        ),
//                        singleLine = true
//                    )
//
//                    Icon(
//                        imageVector = Icons.Default.Search,
//                        contentDescription = "بحث",
//                        tint = extraColors.blue1,
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            // Ships List
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(16.dp)
//            ) {
//                itemsIndexed(
//                    items = filteredShips,
//                    key = { _, ship -> ship.id }
//                ) { index, ship ->
//                    GlassShipCard(
//                        ship = ship,
//                        isSelected = selectedShips.contains(ship.id),
//                        onCardClick = {
//                            if (selectedShips.contains(ship.id)) {
//                                selectedShips.remove(ship.id)
//                            } else {
//                                selectedShips.add(ship.id)
//                            }
//                        },
//                        onShowDetails = {
//                            selectedShipForDetails = ship
//                            showBottomSheet = true
//                        }
//                    )
//                }
//
//                if (filteredShips.isEmpty()) {
//                    item {
//                        EmptyStateView()
//                    }
//                }
//            }
//        }
//    }
//
//    if (showBottomSheet && selectedShipForDetails != null) {
//        ModalBottomSheet(
//            onDismissRequest = {
//                showBottomSheet = false
//                selectedShipForDetails = null
//            },
//            sheetState = sheetState,
//            containerColor = Color(0xFFF8F9FA),
//            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
//            dragHandle = {
//                Box(
//                    modifier = Modifier
//                        .padding(vertical = 16.dp)
//                        .width(50.dp)
//                        .height(5.dp)
//                        .clip(RoundedCornerShape(3.dp))
//                        .background(Color.Gray.copy(alpha = 0.4f))
//                )
//            }
//        ) {
//            GlassBottomSheetContent(
//                ship = selectedShipForDetails!!,
//                onClose = {
//                    scope.launch {
//                        sheetState.hide()
//                        showBottomSheet = false
//                        selectedShipForDetails = null
//                    }
//                }
//            )
//        }
//    }
//}
//
//@Composable
//fun GlassShipCard(
//    ship: Ship,
//    isSelected: Boolean,
//    onCardClick: () -> Unit,
//    onShowDetails: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(
//                onClick = onCardClick,
//                indication = null,
//                interactionSource = remember { MutableInteractionSource() }
//            ),
//        shape = RoundedCornerShape(28.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isSelected)
//                extraColors.blue1.copy(alpha = 0.12f)
//            else
//                Color.White
//        ),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = if (isSelected) 12.dp else 3.dp
//        )
//    ) {
//        Box(modifier = Modifier.fillMaxWidth()) {
//            // Animated Selection Indicator Line
//            this@Card.AnimatedVisibility(
//                visible = isSelected,
//                enter = expandHorizontally() + fadeIn(),
//                exit = shrinkHorizontally() + fadeOut()
//            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(6.dp)
//                        .background(
//                            Brush.horizontalGradient(
//                                colors = listOf(
//                                    Color(0xFF0A2463),
//                                    Color(0xFF00B4CC),
//                                    Color(0xFF5CE1E6)
//                                )
//                            ),
//                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
//                        )
//                )
//            }
//
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(24.dp)
//            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    // Animated Check Circle
//                    Box(
//                        modifier = Modifier
//                            .size(50.dp)
//                            .shadow(
//                                elevation = if (isSelected) 8.dp else 0.dp,
//                                shape = CircleShape,
//                                ambientColor = extraColors.blue1,
//                                spotColor = extraColors.blue1
//                            )
//                            .background(
//                                brush = if (isSelected) {
//                                    Brush.linearGradient(
//                                        colors = listOf(
//                                            Color(0xFF0A2463),
//                                            Color(0xFF00B4CC)
//                                        )
//                                    )
//                                } else {
//                                    Brush.linearGradient(
//                                        colors = listOf(
//                                            Color.Gray.copy(alpha = 0.15f),
//                                            Color.Gray.copy(alpha = 0.1f)
//                                        )
//                                    )
//                                },
//                                shape = CircleShape
//                            )
//                            .clickable(onClick = onCardClick),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        this@Row.AnimatedVisibility(
//                            visible = isSelected,
//                            enter = scaleIn() + fadeIn(),
//                            exit = scaleOut() + fadeOut()
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.CheckCircle,
//                                contentDescription = null,
//                                tint = Color.White,
//                                modifier = Modifier.size(28.dp)
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.width(16.dp))
//
//                    // Ship Info
//                    Column(
//                        modifier = Modifier.weight(1f),
//                        horizontalAlignment = Alignment.End
//                    ) {
//                        Text(
//                            text = ship.name,
//                            fontSize = 19.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = if (isSelected) extraColors.blue1 else Color.Black
//                        )
//                        Spacer(modifier = Modifier.height(6.dp))
//                        Text(
//                            text = ship.type,
//                            fontSize = 14.sp,
//                            color = Color.Gray,
//                            fontWeight = FontWeight.Medium
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.width(16.dp))
//
//                    // Ship Icon with Gradient
//                    Box(
//                        modifier = Modifier
//                            .size(70.dp)
//                            .clip(RoundedCornerShape(20.dp))
//                            .background(
//                                Brush.radialGradient(
//                                    colors = listOf(
//                                        Color(0xFF0A2463).copy(alpha = 0.2f),
//                                        Color(0xFF00B4CC).copy(alpha = 0.1f)
//                                    )
//                                )
//                            ),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            painter = painterResource(R.drawable.ic_launcher_foreground),
//                            contentDescription = null,
//                            tint = extraColors.blue1,
//                            modifier = Modifier.size(45.dp)
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                // Elegant Info Row
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceEvenly
//                ) {
//                    MiniInfoBadge(
//                        label = ship.imoNumber.takeLast(4),
//                        sublabel = "IMO"
//                    )
//                    Box(
//                        modifier = Modifier
//                            .width(1.dp)
//                            .height(40.dp)
//                            .background(Color.Gray.copy(alpha = 0.2f))
//                    )
//                    MiniInfoBadge(
//                        label = ship.callSign,
//                        sublabel = "رمز النداء"
//                    )
//                    Box(
//                        modifier = Modifier
//                            .width(1.dp)
//                            .height(40.dp)
//                            .background(Color.Gray.copy(alpha = 0.2f))
//                    )
//                    MiniInfoBadge(
//                        label = ship.registrationPort,
//                        sublabel = "الميناء"
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                // View Details Button
//                Button(
//                    onClick = onShowDetails,
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(16.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = if (isSelected)
//                            extraColors.blue1
//                        else
//                            Color(0xFFF0F0F0)
//                    ),
//                    elevation = ButtonDefaults.buttonElevation(
//                        defaultElevation = 0.dp
//                    )
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Visibility,
//                        contentDescription = null,
//                        tint = if (isSelected) Color.White else extraColors.blue1,
//                        modifier = Modifier.size(18.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = "عرض التفاصيل الكاملة",
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = if (isSelected) Color.White else extraColors.blue1,
//                        modifier = Modifier.padding(vertical = 8.dp)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun MiniInfoBadge(label: String, sublabel: String) {
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier.padding(horizontal = 8.dp)
//    ) {
//        Text(
//            text = label,
//            fontSize = 16.sp,
//            fontWeight = FontWeight.Bold,
//            color = Color.Black
//        )
//        Text(
//            text = sublabel,
//            fontSize = 11.sp,
//            color = Color.Gray,
//            fontWeight = FontWeight.Medium
//        )
//    }
//}
//
//@Composable
//fun EmptyStateView() {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 60.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(100.dp)
//                    .clip(CircleShape)
//                    .background(Color.Gray.copy(alpha = 0.1f)),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.SearchOff,
//                    contentDescription = null,
//                    modifier = Modifier.size(50.dp),
//                    tint = Color.Gray.copy(alpha = 0.4f)
//                )
//            }
//            Text(
//                text = "لا توجد نتائج",
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.Gray
//            )
//            Text(
//                text = "جرب البحث بكلمات أخرى",
//                fontSize = 14.sp,
//                color = Color.Gray.copy(alpha = 0.7f)
//            )
//        }
//    }
//}
//
//@Composable
//fun GlassBottomSheetContent(ship: Ship, onClose: () -> Unit) {
//    val extraColors = LocalExtraColors.current
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(bottom = 40.dp)
//    ) {
//        // Header with Close Button
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 24.dp, vertical = 12.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(
//                onClick = onClose,
//                modifier = Modifier
//                    .size(45.dp)
//                    .clip(CircleShape)
//                    .background(Color.White)
//                    .shadow(4.dp, CircleShape)
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Close,
//                    contentDescription = "إغلاق",
//                    tint = Color.Gray
//                )
//            }
//
//            Text(
//                text = "معلومات كاملة",
//                fontSize = 22.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.Black
//            )
//        }
//
//        Spacer(modifier = Modifier.height(20.dp))
//
//        LazyColumn(
//            modifier = Modifier.fillMaxWidth(),
//            contentPadding = PaddingValues(horizontal = 24.dp),
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//            // Hero Card
//            item {
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(24.dp),
//                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(
//                                Brush.linearGradient(
//                                    colors = listOf(
//                                        Color(0xFF0A2463),
//                                        Color(0xFF1A5F7A),
//                                        Color(0xFF00B4CC)
//                                    )
//                                )
//                            )
//                            .padding(28.dp)
//                    ) {
//                        Column {
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.SpaceBetween,
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Box(
//                                    modifier = Modifier
//                                        .size(85.dp)
//                                        .clip(CircleShape)
//                                        .background(Color.White.copy(alpha = 0.25f)),
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    Icon(
//                                        painter = painterResource(R.drawable.ic_launcher_foreground),
//                                        contentDescription = null,
//                                        tint = Color.White,
//                                        modifier = Modifier.size(55.dp)
//                                    )
//                                }
//
//                                Column(horizontalAlignment = Alignment.End) {
//                                    Text(
//                                        text = ship.name,
//                                        fontSize = 26.sp,
//                                        fontWeight = FontWeight.Bold,
//                                        color = Color.White
//                                    )
//                                    Spacer(modifier = Modifier.height(6.dp))
//                                    Text(
//                                        text = ship.type,
//                                        fontSize = 16.sp,
//                                        color = Color.White.copy(alpha = 0.9f),
//                                        fontWeight = FontWeight.Medium
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            // Sections
//            item {
//                GlassDetailSection(
//                    title = "المعلومات الأساسية",
//                    emoji = "🚢",
//                    items = listOf(
//                        "رقم IMO" to ship.imoNumber,
//                        "رمز النداء" to ship.callSign,
//                        "رقم الهوية البحرية" to ship.maritimeId
//                    ),
//                    gradientColors = listOf(Color(0xFF0A2463), Color(0xFF1A5F7A))
//                )
//            }
//
//            item {
//                GlassDetailSection(
//                    title = "التسجيل والنشاط",
//                    emoji = "📍",
//                    items = listOf(
//                        "ميناء التسجيل" to ship.registrationPort,
//                        "النشاط البحري" to ship.maritimeActivity,
//                        "بلد التسجيل" to "سلطنة عمان"
//                    ),
//                    gradientColors = listOf(Color(0xFF00B4CC), Color(0xFF5CE1E6))
//                )
//            }
//
//            item {
//                GlassDetailSection(
//                    title = "المواصفات التقنية",
//                    emoji = "⚙️",
//                    items = listOf(
//                        "سنة الصنع" to "2020",
//                        "نوع المحرك" to "ديزل",
//                        "القوة" to "500 HP"
//                    ),
//                    gradientColors = listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
//                )
//            }
//
//            item {
//                GlassDetailSection(
//                    title = "الأبعاد والحمولة",
//                    emoji = "📐",
//                    items = listOf(
//                        "الطول" to "25 متر",
//                        "العرض" to "8 متر",
//                        "العمق" to "4 متر",
//                        "الحمولة الإجمالية" to "150 طن"
//                    ),
//                    gradientColors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun GlassDetailSection(
//    title: String,
//    emoji: String,
//    items: List<Pair<String, String>>,
//    gradientColors: List<Color>
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        elevation = CardDefaults.cardElevation(2.dp)
//    ) {
//        Column(modifier = Modifier.padding(24.dp)) {
//            // Section Header
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = emoji,
//                    fontSize = 32.sp
//                )
//
//                Text(
//                    text = title,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = gradientColors[0]
//                )
//            }
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            // Items
//            items.forEachIndexed { index, (label, mortgageValue) ->
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 12.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text(
//                        text = mortgageValue,
//                        fontSize = 15.sp,
//                        color = Color.Black,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                    Text(
//                        text = label,
//                        fontSize = 15.sp,
//                        color = Color.Gray,
//                        fontWeight = FontWeight.Medium
//                    )
//                }
//
//                if (index < items.size - 1) {
//                    Divider(
//                        color = Color.Gray.copy(alpha = 0.15f),
//                        thickness = 1.dp
//                    )
//                }
//            }
//        }
//    }
//}


//package com.informatique.mtcit.ui.screens
//
//import androidx.compose.animation.*
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.rotate
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.informatique.mtcit.R
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//import kotlinx.coroutines.launch
//
//data class Ship(
//    val id: String,
//    val name: String,
//    val type: String,
//    val imoNumber: String,
//    val callSign: String,
//    val maritimeId: String,
//    val registrationPort: String,
//    val maritimeActivity: String
//)
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ShipDimensionsChangeScreen(navController: NavController) {
//    val extraColors = LocalExtraColors.current
//    var searchQuery by remember { mutableStateOf("") }
//    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//    val scope = rememberCoroutineScope()
//    var showBottomSheet by remember { mutableStateOf(false) }
//    var selectedShipForDetails by remember { mutableStateOf<Ship?>(null) }
//    val selectedShips = remember { mutableStateListOf<String>() }
//
//    val ships = remember {
//        listOf(
//            Ship("1", "الزايدة البحرية", "سفينة شحن", "9900001", "A9BC2", "470123456", "صحار", "نقل بضائع"),
//            Ship("2", "نسيم العرب", "قارب صيد", "9900002", "B8CD3", "470123457", "مسقط", "صيد"),
//            Ship("3", "البحر الأزرق", "ناقلة نفط", "9900003", "C7DE4", "470123458", "الدقم", "نقل نفط"),
//            Ship("4", "موج السلطان", "سفينة ركاب", "9900004", "D6EF5", "470123459", "صلالة", "نقل ركاب")
//        )
//    }
//
//    val filteredShips = remember(searchQuery, ships) {
//        if (searchQuery.isBlank()) ships
//        else ships.filter {
//            it.name.contains(searchQuery, ignoreCase = true) ||
//                    it.imoNumber.contains(searchQuery) ||
//                    it.callSign.contains(searchQuery, ignoreCase = true)
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xFFF5F7FA))
//    ) {
//        Column(modifier = Modifier.fillMaxSize()) {
//            // Modern Header
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(
//                        Brush.horizontalGradient(
//                            colors = listOf(
//                                Color(0xFF6366F1),
//                                Color(0xFF8B5CF6),
//                                Color(0xFFA855F7)
//                            )
//                        )
//                    )
//                    .padding(horizontal = 20.dp, vertical = 24.dp)
//            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    // Selection Counter Badge
//                    AnimatedVisibility(
//                        visible = selectedShips.isNotEmpty(),
//                        enter = scaleIn() + fadeIn(),
//                        exit = scaleOut() + fadeOut()
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .clip(RoundedCornerShape(30.dp))
//                                .background(Color.White.copy(alpha = 0.25f))
//                                .clickable { selectedShips.clear() }
//                                .padding(horizontal = 16.dp, vertical = 8.dp)
//                        ) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(8.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Close,
//                                    contentDescription = null,
//                                    tint = Color.White,
//                                    modifier = Modifier.size(18.dp)
//                                )
//                                Text(
//                                    text = "${selectedShips.size}",
//                                    color = Color.White,
//                                    fontSize = 16.sp,
//                                    fontWeight = FontWeight.Bold
//                                )
//                            }
//                        }
//                    }
//
//                    Column(
//                        horizontalAlignment = Alignment.End
//                    ) {
//                        Text(
//                            text = "السفن المملوكة",
//                            fontSize = 24.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = Color.White
//                        )
//                        Text(
//                            text = if (selectedShips.isEmpty()) "اختر السفن للتعديل" else "تم التحديد بنجاح",
//                            fontSize = 13.sp,
//                            color = Color.White.copy(alpha = 0.85f),
//                            fontWeight = FontWeight.Medium
//                        )
//                    }
//
//                    IconButton(
//                        onClick = { navController.navigateUp() },
//                        modifier = Modifier
//                            .size(44.dp)
//                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
//                    ) {
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                            contentDescription = "رجوع",
//                            tint = Color.White
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Modern Search Bar
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 20.dp)
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clip(RoundedCornerShape(16.dp))
//                        .background(Color.White)
//                        .border(
//                            width = 2.dp,
//                            color = Color(0xFFE5E7EB),
//                            shape = RoundedCornerShape(16.dp)
//                        )
//                        .padding(horizontal = 16.dp, vertical = 14.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Search,
//                        contentDescription = null,
//                        tint = Color(0xFF9CA3AF),
//                        modifier = Modifier.size(22.dp)
//                    )
//
//                    TextField(
//                        mortgageValue = searchQuery,
//                        onValueChange = { searchQuery = it },
//                        modifier = Modifier.weight(1f),
//                        placeholder = {
//                            Text(
//                                text = "ابحث عن سفينة...",
//                                color = Color(0xFF9CA3AF),
//                                fontSize = 15.sp
//                            )
//                        },
//                        colors = TextFieldDefaults.colors(
//                            focusedContainerColor = Color.Transparent,
//                            unfocusedContainerColor = Color.Transparent,
//                            focusedIndicatorColor = Color.Transparent,
//                            unfocusedIndicatorColor = Color.Transparent,
//                            cursorColor = Color(0xFF6366F1)
//                        ),
//                        singleLine = true
//                    )
//
//                    if (searchQuery.isNotEmpty()) {
//                        IconButton(
//                            onClick = { searchQuery = "" },
//                            modifier = Modifier.size(28.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Close,
//                                contentDescription = "مسح",
//                                tint = Color(0xFF9CA3AF),
//                                modifier = Modifier.size(18.dp)
//                            )
//                        }
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            // Ships Grid
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(16.dp)
//            ) {
//                itemsIndexed(
//                    items = filteredShips,
//                    key = { _, ship -> ship.id }
//                ) { _, ship ->
//                    ModernShipCard(
//                        ship = ship,
//                        isSelected = selectedShips.contains(ship.id),
//                        onCardClick = {
//                            if (selectedShips.contains(ship.id)) {
//                                selectedShips.remove(ship.id)
//                            } else {
//                                selectedShips.add(ship.id)
//                            }
//                        },
//                        onShowDetails = {
//                            selectedShipForDetails = ship
//                            showBottomSheet = true
//                        }
//                    )
//                }
//
//                if (filteredShips.isEmpty()) {
//                    item {
//                        ModernEmptyState()
//                    }
//                }
//            }
//        }
//    }
//
//    if (showBottomSheet && selectedShipForDetails != null) {
//        ModalBottomSheet(
//            onDismissRequest = {
//                showBottomSheet = false
//                selectedShipForDetails = null
//            },
//            sheetState = sheetState,
//            containerColor = Color(0xFFF5F7FA),
//            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
//            dragHandle = {
//                Box(
//                    modifier = Modifier
//                        .padding(vertical = 12.dp)
//                        .width(40.dp)
//                        .height(4.dp)
//                        .clip(RoundedCornerShape(2.dp))
//                        .background(Color(0xFFD1D5DB))
//                )
//            }
//        ) {
//            ModernBottomSheetContent(
//                ship = selectedShipForDetails!!,
//                onClose = {
//                    scope.launch {
//                        sheetState.hide()
//                        showBottomSheet = false
//                        selectedShipForDetails = null
//                    }
//                }
//            )
//        }
//    }
//}
//
//@Composable
//fun ModernShipCard(
//    ship: Ship,
//    isSelected: Boolean,
//    onCardClick: () -> Unit,
//    onShowDetails: () -> Unit
//) {
//    val scale by animateFloatAsState(
//        targetValue = if (isSelected) 1.02f else 1f,
//        animationSpec = spring(stiffness = Spring.StiffnessMedium)
//    )
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .scale(scale)
//            .clickable(
//                onClick = onCardClick,
//                indication = null,
//                interactionSource = remember { MutableInteractionSource() }
//            ),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isSelected) Color(0xFFF3F4F6) else Color.White
//        ),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = if (isSelected) 8.dp else 2.dp
//        )
//    ) {
//        Box {
//            // Selection Gradient Border
//            this@Card.AnimatedVisibility(
//                visible = isSelected,
//                enter = fadeIn() + expandVertically(),
//                exit = fadeOut() + shrinkVertically()
//            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(4.dp)
//                        .background(
//                            Brush.horizontalGradient(
//                                colors = listOf(
//                                    Color(0xFF6366F1),
//                                    Color(0xFF8B5CF6),
//                                    Color(0xFFA855F7)
//                                )
//                            )
//                        )
//                )
//            }
//
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(20.dp)
//            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.Top
//                ) {
//                    // Ship Type Badge
//                    Box(
//                        modifier = Modifier
//                            .clip(RoundedCornerShape(12.dp))
//                            .background(
//                                if (isSelected)
//                                    Brush.linearGradient(
//                                        colors = listOf(
//                                            Color(0xFF6366F1),
//                                            Color(0xFF8B5CF6)
//                                        )
//                                    )
//                                else
//                                    Brush.linearGradient(
//                                        colors = listOf(
//                                            Color(0xFFF3F4F6),
//                                            Color(0xFFE5E7EB)
//                                        )
//                                    )
//                            )
//                            .padding(horizontal = 16.dp, vertical = 10.dp)
//                    ) {
//                        Text(
//                            text = ship.type,
//                            fontSize = 13.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            color = if (isSelected) Color.White else Color(0xFF6B7280)
//                        )
//                    }
//
//                    Column(
//                        horizontalAlignment = Alignment.End,
//                        modifier = Modifier.weight(1f).padding(start = 12.dp)
//                    ) {
//                        Text(
//                            text = ship.name,
//                            fontSize = 20.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = if (isSelected) Color(0xFF6366F1) else Color(0xFF111827)
//                        )
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Text(
//                            text = "IMO ${ship.imoNumber}",
//                            fontSize = 13.sp,
//                            color = Color(0xFF9CA3AF),
//                            fontWeight = FontWeight.Medium
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.width(12.dp))
//
//                    // Checkbox
//                    Box(
//                        modifier = Modifier
//                            .size(32.dp)
//                            .clip(CircleShape)
//                            .background(
//                                if (isSelected)
//                                    Brush.linearGradient(
//                                        colors = listOf(
//                                            Color(0xFF6366F1),
//                                            Color(0xFF8B5CF6)
//                                        )
//                                    )
//                                else
//                                    Brush.linearGradient(
//                                        colors = listOf(
//                                            Color(0xFFF3F4F6),
//                                            Color(0xFFE5E7EB)
//                                        )
//                                    )
//                            )
//                            .clickable(onClick = onCardClick),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        this@Row.AnimatedVisibility(
//                            visible = isSelected,
//                            enter = scaleIn() + fadeIn(),
//                            exit = scaleOut() + fadeOut()
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Check,
//                                contentDescription = null,
//                                tint = Color.White,
//                                modifier = Modifier.size(18.dp)
//                            )
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Info Tags
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    InfoTag(
//                        icon = Icons.Default.Place,
//                        text = ship.registrationPort,
//                        isSelected = isSelected
//                    )
//                    InfoTag(
//                        icon = Icons.Default.Call,
//                        text = ship.callSign,
//                        isSelected = isSelected
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Details Button
//                OutlinedButton(
//                    onClick = onShowDetails,
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(12.dp),
//                    border = BorderStroke(
//                        2.dp,
//                        if (isSelected) Color(0xFF6366F1) else Color(0xFFE5E7EB)
//                    ),
//                    colors = ButtonDefaults.outlinedButtonColors(
//                        containerColor = if (isSelected) Color(0xFFF3F4F6) else Color.White
//                    )
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Info,
//                        contentDescription = null,
//                        tint = if (isSelected) Color(0xFF6366F1) else Color(0xFF6B7280),
//                        modifier = Modifier.size(18.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = "التفاصيل الكاملة",
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = if (isSelected) Color(0xFF6366F1) else Color(0xFF6B7280)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun InfoTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, isSelected: Boolean) {
//    Row(
//        modifier = Modifier
//            .clip(RoundedCornerShape(10.dp))
//            .background(
//                if (isSelected) Color.White.copy(alpha = 0.8f)
//                else Color(0xFFF9FAFB)
//            )
//            .padding(horizontal = 12.dp, vertical = 8.dp),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(6.dp)
//    ) {
//        Icon(
//            imageVector = icon,
//            contentDescription = null,
//            tint = if (isSelected) Color(0xFF6366F1) else Color(0xFF9CA3AF),
//            modifier = Modifier.size(16.dp)
//        )
//        Text(
//            text = text,
//            fontSize = 13.sp,
//            fontWeight = FontWeight.Medium,
//            color = Color(0xFF374151)
//        )
//    }
//}
//
//@Composable
//fun ModernEmptyState() {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 80.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(120.dp)
//                    .clip(CircleShape)
//                    .background(
//                        Brush.radialGradient(
//                            colors = listOf(
//                                Color(0xFFEEF2FF),
//                                Color(0xFFE0E7FF)
//                            )
//                        )
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.SearchOff,
//                    contentDescription = null,
//                    modifier = Modifier.size(56.dp),
//                    tint = Color(0xFF6366F1)
//                )
//            }
//            Text(
//                text = "لم يتم العثور على نتائج",
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color(0xFF111827)
//            )
//            Text(
//                text = "حاول استخدام كلمات بحث مختلفة",
//                fontSize = 14.sp,
//                color = Color(0xFF6B7280)
//            )
//        }
//    }
//}
//
//@Composable
//fun ModernBottomSheetContent(ship: Ship, onClose: () -> Unit) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(bottom = 32.dp)
//    ) {
//        // Header
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 24.dp, vertical = 16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(
//                onClick = onClose,
//                modifier = Modifier
//                    .size(40.dp)
//                    .background(Color.White, CircleShape)
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Close,
//                    contentDescription = "إغلاق",
//                    tint = Color(0xFF6B7280)
//                )
//            }
//
//            Text(
//                text = "تفاصيل السفينة",
//                fontSize = 22.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color(0xFF111827)
//            )
//        }
//
//        LazyColumn(
//            modifier = Modifier.fillMaxWidth(),
//            contentPadding = PaddingValues(24.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Hero Section
//            item {
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(20.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = Color.Transparent
//                    )
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(
//                                Brush.linearGradient(
//                                    colors = listOf(
//                                        Color(0xFF6366F1),
//                                        Color(0xFF8B5CF6)
//                                    )
//                                )
//                            )
//                            .padding(24.dp)
//                    ) {
//                        Column(
//                            horizontalAlignment = Alignment.End
//                        ) {
//                            Text(
//                                text = ship.name,
//                                fontSize = 28.sp,
//                                fontWeight = FontWeight.Bold,
//                                color = Color.White
//                            )
//                            Spacer(modifier = Modifier.height(8.dp))
//                            Row(
//                                horizontalArrangement = Arrangement.spacedBy(12.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Text(
//                                    text = ship.type,
//                                    fontSize = 16.sp,
//                                    color = Color.White.copy(alpha = 0.9f),
//                                    fontWeight = FontWeight.Medium
//                                )
//                                Box(
//                                    modifier = Modifier
//                                        .size(4.dp)
//                                        .clip(CircleShape)
//                                        .background(Color.White.copy(alpha = 0.6f))
//                                )
//                                Text(
//                                    text = ship.registrationPort,
//                                    fontSize = 16.sp,
//                                    color = Color.White.copy(alpha = 0.9f),
//                                    fontWeight = FontWeight.Medium
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//
//            // Info Sections
//            item {
//                DetailCard(
//                    title = "المعلومات الأساسية",
//                    icon = "🚢",
//                    color = Color(0xFF6366F1),
//                    items = listOf(
//                        "رقم IMO" to ship.imoNumber,
//                        "رمز النداء" to ship.callSign,
//                        "الهوية البحرية" to ship.maritimeId
//                    )
//                )
//            }
//
//            item {
//                DetailCard(
//                    title = "التسجيل والنشاط",
//                    icon = "📍",
//                    color = Color(0xFF10B981),
//                    items = listOf(
//                        "ميناء التسجيل" to ship.registrationPort,
//                        "النشاط البحري" to ship.maritimeActivity,
//                        "بلد التسجيل" to "سلطنة عمان"
//                    )
//                )
//            }
//
//            item {
//                DetailCard(
//                    title = "المواصفات",
//                    icon = "⚙️",
//                    color = Color(0xFFF59E0B),
//                    items = listOf(
//                        "سنة الصنع" to "2020",
//                        "نوع المحرك" to "ديزل",
//                        "القوة" to "500 HP"
//                    )
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun DetailCard(
//    title: String,
//    icon: String,
//    color: Color,
//    items: List<Pair<String, String>>
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        elevation = CardDefaults.cardElevation(2.dp)
//    ) {
//        Column(modifier = Modifier.padding(20.dp)) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Box(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(12.dp))
//                        .background(color.copy(alpha = 0.1f))
//                        .padding(12.dp)
//                ) {
//                    Text(text = icon, fontSize = 24.sp)
//                }
//
//                Text(
//                    text = title,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = color
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            items.forEachIndexed { index, (label, mortgageValue) ->
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 10.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = mortgageValue,
//                        fontSize = 15.sp,
//                        color = Color(0xFF111827),
//                        fontWeight = FontWeight.SemiBold
//                    )
//                    Text(
//                        text = label,
//                        fontSize = 14.sp,
//                        color = Color(0xFF6B7280),
//                        fontWeight = FontWeight.Medium
//                    )
//                }
//
//                if (index < items.size - 1) {
//                    Divider(
//                        color = Color(0xFFF3F4F6),
//                        thickness = 1.dp
//                    )
//                }
//            }
//        }
//    }
//}
