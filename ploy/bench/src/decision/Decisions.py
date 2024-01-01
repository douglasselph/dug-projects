from src.decision.base.BasePlaceCard import BasePlaceCard
from src.decision.base.BaseTrash import BaseTrash
from src.decision.base.BaseDeployChooseCard import BaseDeployChooseCard
from src.decision.base.BaseDeployBlock import BaseDeployBlock
from src.decision.base.BaseFeelingFeint import BaseFeelingFeint
from src.decision.base.BaseRevealSupportingLine import BaseRevealSupportingLine
from src.decision.validate.ValidatePlaceCard import ValidatePlaceCard
from src.decision.validate.ValidateTrash import ValidateTrash
from src.decision.validate.ValidateDeployChooseCard import ValidateDeployChooseCard
from src.decision.validate.ValidateDeployBlock import ValidateDeployBlock
from src.decision.validate.ValidateFeelingFeint import ValidateFeelingFeint
from src.decision.validate.ValidateRevealSupportingLine import ValidateRevealSupportingLine


class Decisions:

    placeCard: BasePlaceCard
    trash: BaseTrash
    deployChooseCard: BaseDeployChooseCard
    deployBlock: BaseDeployBlock
    feelingFeint: BaseFeelingFeint
    revealSupportingLine: BaseRevealSupportingLine

    def __init__(self):
        self.placeCard = ValidatePlaceCard()
        self.trash = ValidateTrash()
        self.deployChooseCard = ValidateDeployChooseCard()
        self.deployBlock = ValidateDeployBlock()
        self.feelingFeint = ValidateFeelingFeint()
        self.revealSupportingLine = ValidateRevealSupportingLine()

