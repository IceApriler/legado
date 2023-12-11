package io.legado.app.data.entities

import android.content.Context
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.help.config.AppConfig
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "collect_tag")
data class CollectTag(
    @PrimaryKey
    val tagId: Long = 0b1,
    var tagName: String
) : Parcelable {
    override fun hashCode(): Int {
        return tagId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is CollectTag) {
            return other.tagId == tagId
                    && other.tagName == tagName
        }
        return false
    }

}