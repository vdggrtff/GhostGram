package entity

data class ChatInvitePreview(
    val inviteLink: String,
    val title: String,
    val description: String,
    val memberCount: Int,
    val isChannel: Boolean = false,
    val createsJoinRequest: Boolean = false // Заявка на вступление или мгновенный вход
)

sealed interface InviteLinkResult {
    data class AlreadyJoined(val chatId: Long) : InviteLinkResult // Уже состоим -> сразу открываем
    data class NeedsJoin(val preview: ChatInvitePreview) : InviteLinkResult // Показываем диалог
    data class Error(val message: String) : InviteLinkResult
}