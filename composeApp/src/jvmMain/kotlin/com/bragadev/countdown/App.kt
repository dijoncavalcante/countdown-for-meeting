package com.bragadev.countdown

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

    // ─── Cores do tema ───────────────────────────────────────────────────────────
    private val BackgroundDark  = Color(0xFF0A0E1A)
    private val BackgroundMid   = Color(0xFF0F1629)
    private val AccentBlue      = Color(0xFF1E90FF)
    private val AccentCyan      = Color(0xFF00D4FF)
    private val AccentGlow      = Color(0xFF0A4A8A)
    private val TextPrimary     = Color(0xFFECF4FF)
    private val TextSecondary   = Color(0xFF7BA7D4)
    private val DangerRed       = Color(0xFFFF4560)
    private val SuccessGreen    = Color(0xFF00E396)

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Retorna o horário alvo da reunião para HOJE:
     *  - Segunda a Sexta → 19:30
     *  - Sábado e Domingo → 19:00
     */
    fun meetingTimeToday(): LocalDateTime {
        val now = LocalDateTime.now()
        val meetingTime = when (now.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> LocalTime.of(19, 0)
            else -> LocalTime.of(19,30)
        }
        return now.toLocalDate().atTime(meetingTime)
    }

    /**
     * Calcula os segundos restantes até a reunião.
     * Retorna 0 se já passou do horário.
     */
    fun secondsUntilMeeting(): Long {
        val now = LocalDateTime.now()
        val target = meetingTimeToday()
        return maxOf(0L, ChronoUnit.SECONDS.between(now, target))
    }

    fun formatTime(totalSeconds: Long): Triple<String, String, String> {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return Triple(
            h.toString().padStart(2, '0'),
            m.toString().padStart(2, '0'),
            s.toString().padStart(2, '0')
        )
    }

    // ─── Tela principal ───────────────────────────────────────────────────────────

    @Composable
    fun TimerScreen(onClose: () -> Unit) {

        // Estados
        var running        by remember { mutableStateOf(false) }
        var finished       by remember { mutableStateOf(false) }
        var secondsLeft    by remember { mutableStateOf(0L) }

        // Animação de pulso para o timer quando rodando
        val pulse by animateFloatAsState(
            targetValue  = if (running && !finished) 1.03f else 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        // Animação de escala para a mensagem final
        val finishScale by animateFloatAsState(
            targetValue  = if (finished) 1f else 0.5f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "finishScale"
        )

        // Alpha piscando para indicação de urgência (< 60s)
        val urgentAlpha by animateFloatAsState(
            targetValue  = if (running && secondsLeft in 1..59) 0.4f else 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "urgentAlpha"
        )

        // Countdown coroutine
        LaunchedEffect(running) {
            if (running && !finished) {
                while (secondsLeft > 0) {
                    delay(1000L)
                    secondsLeft--
                }
                finished = true
                running  = false
            }
        }

        val (hh, mm, ss) = formatTime(secondsLeft)

        // Cor dinâmica do dígito conforme o tempo
        val digitColor = when {
            finished          -> SuccessGreen
            secondsLeft < 60  -> DangerRed
            secondsLeft < 300 -> Color(0xFFFFB800)
            else              -> AccentCyan
        }

        // Fundo gradiente escuro
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentGlow, BackgroundMid, BackgroundDark),
                        radius = 1200f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.padding(40.dp)
            ) {

                // ── Título ──────────────────────────────────────────────────────
                Text(
                    text      = "⏱ A reunião começará em:",
                    fontSize  = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp,
                    color     = TextSecondary
                )

                // ── Bloco do temporizador / mensagem final ──────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(if (finished) finishScale else pulse)
                ) {
                    if (finished) {
                        // Mensagem de fim
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                            Text(
//                                text      = "🎉",
//                                fontSize  = 96.sp,
//                                textAlign = TextAlign.Center
//                            )
//                            Spacer(Modifier.height(16.dp))
                            Text(
                                text       = "Boa reunião!",
                                fontSize   = 80.sp,
                                fontWeight = FontWeight.Black,
                                color      = TextPrimary,
                                textAlign  = TextAlign.Center,
                                letterSpacing = 2.sp
                            )
                        }
                    } else {
                        // Dígitos HH:MM:SS
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.alpha(
                                if (!running) 1f else urgentAlpha.coerceAtLeast(1f)
                                    .let { if (secondsLeft < 60) urgentAlpha else 1f }
                            )
                        ) {
                            DigitBlock(hh, digitColor)
                            Separator(digitColor)
                            DigitBlock(mm, digitColor)
                            Separator(digitColor)
                            DigitBlock(ss, digitColor)
                        }
                    }
                }

                // ── Subtítulo da reunião ────────────────────────────────────────
                if (!finished) {
                    val dayLabel = when (LocalDateTime.now().dayOfWeek) {
                        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> "Fim de semana • 19:00"
                        else -> "Dias de semana • 19:30"
                    }
                    Text(
                        text      = dayLabel,
                        fontSize  = 18.sp,
                        color     = TextSecondary,
                        letterSpacing = 3.sp
                    )
                }

                // ── Botões ──────────────────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Botão Iniciar / Pausar
                    if (!finished) {
                        Button(
                            onClick = {
                                if (!running) {
                                    // (Re)calcula ao iniciar
                                    secondsLeft = secondsUntilMeeting()
                                }
                                running = !running
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (running) Color(0xFF1A3A5C) else AccentBlue
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .widthIn(min = 160.dp)
                        ) {
                            Text(
                                text       = if (running) "⏸  Pausar" else "▶  Iniciar",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Botão Resetar (só aparece quando pausado e já iniciou)
                        if (!running && secondsLeft > 0) {
                            OutlinedButton(
                                onClick = {
                                    running     = false
                                    secondsLeft = 0L
                                },
                                shape  = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextSecondary
                                ),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("↺  Resetar", fontSize = 18.sp)
                            }
                        }
                    }

                    // Botão Fechar
                    Button(
                        onClick = onClose,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A0A0A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .widthIn(min = 140.dp)
                    ) {
                        Text(
                            text       = "✕  Fechar",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = DangerRed
                        )
                    }
                }
            }
        }
    }

    // ─── Composables auxiliares ───────────────────────────────────────────────────

    @Composable
    private fun DigitBlock(value: String, color: Color) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    color  = Color(0xFF0D1930),
                    shape  = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text       = value,
                fontSize   = 140.sp,
                fontWeight = FontWeight.Black,
                color      = TextPrimary, //color,
                letterSpacing = (-4).sp
            )
        }
    }

    @Composable
    private fun Separator(color: Color) {
        Text(
            text       = ":",
            fontSize   = 120.sp,
            fontWeight = FontWeight.Black,
            color      = TextPrimary, //color.copy(alpha = 0.6f),
            modifier   = Modifier.padding(horizontal = 8.dp)
        )
    }

    @Composable
    fun App(onExit: () -> Unit = {}) {
        MaterialTheme {
            var showTimer by remember { mutableStateOf(true) }

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showTimer) {
                    TimerScreen(onClose = onExit)
                }
            }
        }
    }