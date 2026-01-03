/*
 * Copyright 2022-2026 Virogu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.WindowState
import com.virogu.core.tool.Tools
import com.virogu.ui.Pager
import com.virogu.ui.PagerNavController
import com.virogu.ui.pager.InitPager
import com.virogu.ui.pager.MainPager
import theme.MainTheme

/**
 * @author Virogu
 * @since 2022-09-01 10:50
 **/

@Composable
@Preview
fun App(
    window: ComposeWindow,
    applicationScope: ApplicationScope,
    state: WindowState,
    trayState: TrayState,
    pagerController: PagerNavController<Pager>,
    tools: Tools
) {
    val initState = tools.initTool.initStateFlow.collectAsState()
    MainTheme {
        //AppTitleView(applicationScope, state)
        val (initStateSuccess, _) = remember(initState.value) {
            mutableStateOf(initState.value.success)
        }
        if (initStateSuccess) {
            MainPager(trayState, pagerController, tools)
        } else {
            InitPager(initState)
        }
    }
}

//@Composable
//private fun WindowScope.AppTitleView(
//    applicationScope: ApplicationScope, state: WindowState
//) {
//    WindowDraggableArea {
//        Box(Modifier.fillMaxWidth().height(48.dp)) {
//            Row(Modifier.align(Alignment.CenterEnd)) {
//                Button(
//                    modifier = Modifier.size(50.dp, 40.dp), onClick = {
//                        applicationScope.exitApplication()
//                    }, colors = ButtonDefaults.buttonColors(
//                        backgroundColor = Color.Transparent,
//                        contentColor = Color.White,
//                    ), elevation = ButtonDefaults.elevation(
//                        defaultElevation = 0.dp,
//                        pressedElevation = 0.dp,
//                        disabledElevation = 0.dp,
//                        hoveredElevation = 0.dp,
//                        focusedElevation = 0.dp,
//                    ), contentPadding = PaddingValues(0.dp)
//                ) {
//                    //Image(
//                    //    imageVector = Icons.Filled.Close,
//                    //    contentDescription = "",
//                    //    colorFilter = ColorFilter.tint(Color.White)
//                    //)
//                }
//            }
//        }
//    }
//}
