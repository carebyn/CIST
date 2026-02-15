package kr.go.mohw.cist.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val HOME = "home"
private const val TEST = "test"
private const val SUPPORT = "support"
private const val RESULT = "result/{score}"

private data class CistQuestion(
    val number: Int,
    val text: String
)

// CIST 기반 간편 문항 (실서비스 시 보건복지부 최신 고시 문항으로 교체 필요)
private val questions = listOf(
    CistQuestion(1, "스마트폰이 없으면 불안하고 초조하다."),
    CistQuestion(2, "사용 시간을 줄이려 해도 실패하는 경우가 많다."),
    CistQuestion(3, "수면시간을 줄이면서까지 스마트폰을 사용한다."),
    CistQuestion(4, "집중이 필요할 때도 스마트폰 생각이 난다."),
    CistQuestion(5, "가족/친구와 갈등이 생길 정도로 사용한다."),
    CistQuestion(6, "스마트폰을 사용하면 기분이 즉시 좋아진다."),
    CistQuestion(7, "사용하지 않으면 금단 증상이 느껴진다."),
    CistQuestion(8, "해야 할 일을 미루고 스마트폰을 먼저 본다."),
    CistQuestion(9, "학업/업무 성과가 떨어졌다고 느낀다."),
    CistQuestion(10, "식사/이동 중에도 계속 확인하게 된다."),
    CistQuestion(11, "사용 시간에 대해 주변의 지적을 받은 적이 있다."),
    CistQuestion(12, "짧게 사용하려 했지만 오래 사용하는 편이다."),
    CistQuestion(13, "현실 관계보다 스마트폰 안 관계가 더 편하다."),
    CistQuestion(14, "최근 1년간 사용 시간이 계속 증가했다."),
    CistQuestion(15, "스마트폰 없이 여가를 보내기 어렵다.")
)

@Composable
fun CistApp() {
    val navController = rememberNavController()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF6F8FB)) {
            NavHost(navController = navController, startDestination = HOME) {
                composable(HOME) { HomeScreen(navController) }
                composable(TEST) { TestScreen(navController) }
                composable(SUPPORT) { SupportScreen() }
                composable(RESULT) { backStackEntry ->
                    val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
                    ResultScreen(navController, score)
                }
            }
        }
    }
}

@Composable
private fun AppScaffold(content: @Composable (PaddingValues) -> Unit) {
    Scaffold(bottomBar = { AdBanner() }) { padding -> content(padding) }
}

@Composable
private fun HomeScreen(navController: NavHostController) {
    AppScaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CIST 간편 검사", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("토스 스타일의 깔끔한 UX", color = Color(0xFF5F6B7A), modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
            TossButton(text = "검사하기") { navController.navigate(TEST) }
            TossButton(text = "개발자 후원하기", modifier = Modifier.padding(top = 16.dp)) { navController.navigate(SUPPORT) }
        }
    }
}

@Composable
private fun TestScreen(navController: NavHostController) {
    val answers = remember { mutableStateListOf<Int?>(*Array(questions.size) { null }) }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    AppScaffold { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            LazyColumn(
                state = state,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                itemsIndexed(questions) { index, q ->
                    QuestionCard(question = q, selected = answers[index], onSelect = { answers[index] = it })
                }
                item {
                    Button(
                        onClick = { navController.navigate("result/${calculateScore(answers)}") },
                        enabled = answers.all { it != null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("결과확인") }
                }
            }

            Column(
                modifier = Modifier.padding(start = 10.dp).fillMaxHeight().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                questions.indices.forEach { idx ->
                    val done = answers[idx] != null
                    Box(
                        modifier = Modifier.size(34.dp)
                            .background(if (done) Color(0xFFDCFCE7) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { scope.launch { state.animateScrollToItem(idx) } },
                        contentAlignment = Alignment.Center
                    ) { Text(text = "${idx + 1}", color = Color(0xFF3E4C59)) }
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(navController: NavHostController, score: Int) {
    val (title, detail) = classify(score)
    AppScaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("검사 결과", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("총점: $score / 60", style = MaterialTheme.typography.titleLarge)
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                    Text(detail, color = Color(0xFF5F6B7A), modifier = Modifier.padding(top = 8.dp))
                }
            }
            TossButton(text = "처음으로", modifier = Modifier.padding(top = 24.dp)) {
                navController.navigate(HOME) { popUpTo(HOME) { inclusive = true } }
            }
        }
    }
}

@Composable
private fun SupportScreen() {
    val context = LocalContext.current
    val billing = remember { BillingManager(context) }
    var products by remember { mutableStateOf<List<ProductDetails>>(emptyList()) }

    LaunchedEffect(Unit) { billing.connect { products = billing.loadProducts() } }

    AppScaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState())
        ) {
            Text("개발자 후원하기", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("플레이스토어 인앱결제로 후원할 수 있어요.", color = Color(0xFF5F6B7A), modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            listOf("support_1000" to "₩1,000", "support_5000" to "₩5,000", "support_10000" to "₩10,000", "support_50000" to "₩50,000")
                .forEach { (productId, label) ->
                    TossButton(text = "$label 후원하기") {
                        billing.launchPurchase(products.firstOrNull { it.productId == productId })
                    }
                    Box(modifier = Modifier.height(12.dp))
                }
            Text("Play Console에서 동일 productId 인앱 상품 등록이 필요합니다.", color = Color(0xFF5F6B7A))
        }
    }
}

@Composable
private fun QuestionCard(question: CistQuestion, selected: Int?, onSelect: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("문항 ${question.number}", color = Color(0xFF5F6B7A))
            Text(question.text, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)
            val options = listOf("전혀 아니다", "아니다", "그렇다", "매우 그렇다")
            Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { idx, opt ->
                    val value = idx + 1
                    Button(
                        onClick = { onSelect(value) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected == value) Color(0xFF3182F6) else Color(0xFFE8EEF7),
                            contentColor = if (selected == value) Color.White else Color(0xFF334155)
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) { Text(opt) }
                }
            }
        }
    }
}

@Composable
private fun TossButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182F6), contentColor = Color.White)
    ) { Text(text) }
}

@Composable
private fun AdBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

private fun calculateScore(answers: List<Int?>): Int = answers.sumOf { it ?: 1 }

private fun classify(score: Int): Pair<String, String> = when {
    score >= 44 -> "고위험 사용자군" to "전문기관 상담이 권장됩니다. 사용 통제 어려움과 기능 저하가 뚜렷한 수준입니다."
    score >= 38 -> "잠재적 위험 사용자군" to "사용 습관 교정이 필요합니다. 이용시간 제한과 수면/학습 루틴 회복을 권장합니다."
    else -> "일반 사용자군" to "현재는 비교적 안정적인 수준입니다. 건강한 디지털 습관을 유지하세요."
}

private class BillingManager(private val context: Context) {
    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener { _, _ -> }
        .enablePendingPurchases()
        .build()

    fun connect(onReady: suspend () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() = Unit

            override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    CoroutineScope(Dispatchers.Main).launch { onReady() }
                }
            }
        })
    }

    suspend fun loadProducts(): List<ProductDetails> {
        val products = listOf("support_1000", "support_5000", "support_10000", "support_50000").map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val result = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(products).build()
        )
        return result.productDetailsList ?: emptyList()
    }

    fun launchPurchase(productDetails: ProductDetails?) {
        val activity = context as? Activity ?: return
        val detail = productDetails ?: return
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(detail).build()
        billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
    }
}
