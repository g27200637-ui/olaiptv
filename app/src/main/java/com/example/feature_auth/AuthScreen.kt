package com.example.feature_auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.tvFocusableBorder

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var playlistName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val playlistNameInteractionSource = remember { MutableInteractionSource() }
    val serverUrlInteractionSource = remember { MutableInteractionSource() }
    val usernameInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }
    val loginButtonInteractionSource = remember { MutableInteractionSource() }
    val demoButtonInteractionSource = remember { MutableInteractionSource() }

    // Navigation trigger on Success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(Color(0xFF080808)),
        contentAlignment = Alignment.Center
    ) {
        if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = "IPTV Logo",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(54.dp)
                )

                Text(
                    text = "Ola IPTV",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Smart Server Portal".uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF121212)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AuthInputContent(
                            playlistName = playlistName,
                            onPlaylistNameChange = { playlistName = it },
                            serverUrl = serverUrl,
                            onServerUrlChange = { serverUrl = it },
                            username = username,
                            onUsernameChange = { username = it },
                            password = password,
                            onPasswordChange = { password = it },
                            isPasswordVisible = isPasswordVisible,
                            onIsPasswordVisibleChange = { isPasswordVisible = it },
                            uiState = uiState,
                            viewModel = viewModel,
                            focusManager = focusManager,
                            playlistNameInteractionSource = playlistNameInteractionSource,
                            serverUrlInteractionSource = serverUrlInteractionSource,
                            usernameInteractionSource = usernameInteractionSource,
                            passwordInteractionSource = passwordInteractionSource,
                            loginButtonInteractionSource = loginButtonInteractionSource,
                            demoButtonInteractionSource = demoButtonInteractionSource,
                            isPortrait = true
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.95f),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pane: TV Branding & Design Details
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "IPTV Logo",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier
                            .size(60.dp)
                            .padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Ola IPTV",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Smart Server Portal".uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Experience your live television categories with zero delay and smooth, high-fidelity media decoding. Log in using your Xtream Codes credentials below.",
                        fontSize = 13.sp,
                        color = Color(0xFFE0E0E0).copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }

                // Right Pane: Login inputs
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF121212)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AuthInputContent(
                            playlistName = playlistName,
                            onPlaylistNameChange = { playlistName = it },
                            serverUrl = serverUrl,
                            onServerUrlChange = { serverUrl = it },
                            username = username,
                            onUsernameChange = { username = it },
                            password = password,
                            onPasswordChange = { password = it },
                            isPasswordVisible = isPasswordVisible,
                            onIsPasswordVisibleChange = { isPasswordVisible = it },
                            uiState = uiState,
                            viewModel = viewModel,
                            focusManager = focusManager,
                            playlistNameInteractionSource = playlistNameInteractionSource,
                            serverUrlInteractionSource = serverUrlInteractionSource,
                            usernameInteractionSource = usernameInteractionSource,
                            passwordInteractionSource = passwordInteractionSource,
                            loginButtonInteractionSource = loginButtonInteractionSource,
                            demoButtonInteractionSource = demoButtonInteractionSource,
                            isPortrait = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.AuthInputContent(
    playlistName: String,
    onPlaylistNameChange: (String) -> Unit,
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onIsPasswordVisibleChange: (Boolean) -> Unit,
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    focusManager: FocusManager,
    playlistNameInteractionSource: MutableInteractionSource,
    serverUrlInteractionSource: MutableInteractionSource,
    usernameInteractionSource: MutableInteractionSource,
    passwordInteractionSource: MutableInteractionSource,
    loginButtonInteractionSource: MutableInteractionSource,
    demoButtonInteractionSource: MutableInteractionSource,
    isPortrait: Boolean
) {
    Text(
        text = "Ola IPTV Portal Login",
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    // Row 1: Playlist Name & Server URL (Stacked on Portrait, Row on Landscape)
    if (isPortrait) {
        CustomTvInputField(
            value = playlistName,
            onValueChange = onPlaylistNameChange,
            label = "Playlist Name",
            placeholder = "My IPTV",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            interactionSource = playlistNameInteractionSource,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        CustomTvInputField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            label = "Server URL",
            placeholder = "http://url:port",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            interactionSource = serverUrlInteractionSource,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomTvInputField(
                value = playlistName,
                onValueChange = onPlaylistNameChange,
                label = "Playlist Name",
                placeholder = "My IPTV",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                interactionSource = playlistNameInteractionSource,
                modifier = Modifier.weight(1f)
            )

            CustomTvInputField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                label = "Server URL",
                placeholder = "http://url:port",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                interactionSource = serverUrlInteractionSource,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Row 2: Username & Password (Stacked on Portrait, Row on Landscape)
    if (isPortrait) {
        CustomTvInputField(
            value = username,
            onValueChange = onUsernameChange,
            label = "Username",
            placeholder = "Username",
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            interactionSource = usernameInteractionSource,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        CustomTvInputField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            placeholder = "Password",
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
            trailingIcon = null,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.login(serverUrl, username, password, playlistName)
                }
            ),
            interactionSource = passwordInteractionSource,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomTvInputField(
                value = username,
                onValueChange = onUsernameChange,
                label = "Username",
                placeholder = "Username",
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                interactionSource = usernameInteractionSource,
                modifier = Modifier.weight(1f)
            )

            CustomTvInputField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                placeholder = "Password",
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                trailingIcon = null,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.login(serverUrl, username, password, playlistName)
                    }
                ),
                interactionSource = passwordInteractionSource,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Dedicated D-pad optimized show password toggle row
    val showPassInteractionSource = remember { MutableInteractionSource() }
    val isShowPassFocused by showPassInteractionSource.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .run { if (isPortrait) fillMaxWidth() else align(Alignment.End) }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isShowPassFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(
                interactionSource = showPassInteractionSource,
                indication = null,
                onClick = { onIsPasswordVisibleChange(!isPasswordVisible) }
            )
            .focusable(interactionSource = showPassInteractionSource)
            .tvFocusableBorder(
                interactionSource = showPassInteractionSource,
                shape = RoundedCornerShape(8.dp),
                focusedBorderColor = Color(0xFFEF4444),
                focusedScale = 1.02f,
                focusable = false
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isPortrait) Arrangement.Center else Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = null,
            tint = if (isPasswordVisible) Color(0xFFEF4444) else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = if (isPasswordVisible) " HIDE PASSWORD" else " SHOW PASSWORD",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Error display
    AnimatedVisibility(
        visible = uiState is AuthUiState.Error,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val message = (uiState as? AuthUiState.Error)?.message ?: ""
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }

    // Bottom Buttons & Loading state
    if (uiState is AuthUiState.Loading) {
        CircularProgressIndicator(
            color = Color(0xFFEF4444),
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp)
        )
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login(serverUrl, username, password, playlistName)
                },
                interactionSource = loginButtonInteractionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .tvFocusableBorder(
                        interactionSource = loginButtonInteractionSource,
                        focusedBorderColor = Color.White,
                        focusable = false
                    )
            ) {
                Text(
                    text = "LOG IN",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login("demo_mode", "demo", "demo", "Demo Portal")
                },
                interactionSource = demoButtonInteractionSource,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF4444)
                ),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .tvFocusableBorder(
                        interactionSource = demoButtonInteractionSource,
                        focusedBorderColor = Color.White,
                        focusable = false
                    )
            ) {
                Text(
                    text = "DEMO MODE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CustomTvInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    modifier: Modifier = Modifier
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val borderStroke = if (isFocused) {
        BorderStroke(2.dp, Color(0xFFEF4444))
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    }

    val containerColor = if (isFocused) {
        Color(0xFF1E1E1E)
    } else {
        Color(0xFF141414)
    }

    Column(
        modifier = modifier
            .scale(scale),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = if (isFocused) Color(0xFFEF4444) else Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 2.dp)
        )

        Surface(
            color = containerColor,
            shape = RoundedCornerShape(10.dp),
            border = borderStroke,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (leadingIcon != null) {
                    Box(modifier = Modifier.size(16.dp)) {
                        CompositionLocalProvider(
                            LocalContentColor provides if (isFocused) Color(0xFFEF4444) else Color.White.copy(alpha = 0.4f)
                        ) {
                            leadingIcon()
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.White.copy(alpha = 0.25f),
                            fontSize = 13.sp
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                        cursorBrush = SolidColor(Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}
