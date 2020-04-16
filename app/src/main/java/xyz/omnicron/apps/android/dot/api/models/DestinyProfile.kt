package xyz.omnicron.apps.android.dot.api.models

class DestinyProfile(var membershipType: MembershipType,
                     var membershipId: String,
                     var displayName: String
) {
    var characters: ArrayList<DestinyCharacter> = arrayListOf()
}