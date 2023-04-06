package polyrhythmmania.storymode.inbox

import polyrhythmmania.storymode.contract.IHasContractTextInfo


interface IContractDoc : IHasContractTextInfo {

    enum class ContractSubtype(val headingL10NKey: String) {
        NORMAL("inboxItem.contract.heading.normal"),
        TRAINING("inboxItem.contract.heading.training"),
        ROBOT_TEST("inboxItem.contract.heading.robotTest"),
    }
    
    
    val hasLongCompanyName: Boolean
    val subtype: ContractSubtype
    
}
