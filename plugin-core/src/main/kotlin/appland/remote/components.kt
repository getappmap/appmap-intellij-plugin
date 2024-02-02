package appland.remote

import com.intellij.ui.TextFieldWithHistory
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.columns
import com.intellij.util.ui.SwingHelper

@Suppress("UnstableApiUsage")
internal fun Row.textFieldWithHistory(history: List<String>): Cell<TextFieldWithHistory> {
    val component = TextFieldWithHistory()
    component.setHistorySize(-1)
    component.setMinimumAndPreferredWidth(0)
    SwingHelper.addHistoryOnExpansion(component) { history }

    return cell(component).columns(COLUMNS_LARGE)
}
