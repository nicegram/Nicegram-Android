package org.telegram.tgnet.model.generated

import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.UInt
import kotlin.collections.List
import org.telegram.tgnet.OutputSerializedData
import org.telegram.tgnet.model.TlGen_Object
import org.telegram.tgnet.model.TlGen_Vector

public sealed class TlGen_SavedStarGift : TlGen_Object {
  public data class TL_savedStarGift(
    public val name_hidden: Boolean,
    public val unsaved: Boolean,
    public val refunded: Boolean,
    public val can_upgrade: Boolean,
    public val pinned_to_top: Boolean,
    public val from_id: TlGen_Peer?,
    public val date: Int,
    public val gift: TlGen_StarGift,
    public val message: TlGen_TextWithEntities?,
    public val msg_id: Int?,
    public val saved_id: Long?,
    public val convert_stars: Long?,
    public val upgrade_stars: Long?,
    public val can_export_at: Int?,
    public val transfer_stars: Long?,
    public val can_transfer_at: Int?,
    public val can_resell_at: Int?,
    public val collection_id: List<Int>?,
  ) : TlGen_SavedStarGift() {
    internal val flags: UInt
      get() {
        var result = 0U
        if (name_hidden) result = result or 1U
        if (from_id != null) result = result or 2U
        if (message != null) result = result or 4U
        if (msg_id != null) result = result or 8U
        if (convert_stars != null) result = result or 16U
        if (unsaved) result = result or 32U
        if (upgrade_stars != null) result = result or 64U
        if (can_export_at != null) result = result or 128U
        if (transfer_stars != null) result = result or 256U
        if (refunded) result = result or 512U
        if (can_upgrade) result = result or 1024U
        if (saved_id != null) result = result or 2048U
        if (pinned_to_top) result = result or 4096U
        if (can_transfer_at != null) result = result or 8192U
        if (can_resell_at != null) result = result or 16384U
        if (collection_id != null) result = result or 32768U
        return result
      }

    public override fun serializeToStream(stream: OutputSerializedData) {
      stream.writeInt32(MAGIC.toInt())
      stream.writeInt32(flags.toInt())
      from_id?.serializeToStream(stream)
      stream.writeInt32(date)
      gift.serializeToStream(stream)
      message?.serializeToStream(stream)
      msg_id?.let { stream.writeInt32(it) }
      saved_id?.let { stream.writeInt64(it) }
      convert_stars?.let { stream.writeInt64(it) }
      upgrade_stars?.let { stream.writeInt64(it) }
      can_export_at?.let { stream.writeInt32(it) }
      transfer_stars?.let { stream.writeInt64(it) }
      can_transfer_at?.let { stream.writeInt32(it) }
      can_resell_at?.let { stream.writeInt32(it) }
      collection_id?.let { TlGen_Vector.serializeInt(stream, it) }
    }

    public companion object {
      public const val MAGIC: UInt = 0x1EA646DFU
    }
  }
}
