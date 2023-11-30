from src.decision.base.BasePlaceCard import BasePlaceCard
from src.decision.base.BaseTrash import BaseTrash
from src.decision.base.BaseDeployChooseCard import BaseDeployChooseCard
from src.decision.base.BaseDeployBlock import BaseDeployBlock
from src.decision.validate.ValidatePlaceCard import ValidatePlaceCard
from src.decision.validate.ValidateTrash import ValidateTrash
from src.decision.validate.ValidateDeployChooseCard import ValidateDeployChooseCard
from src.decision.validate.ValidateDeployBlock import ValidateDeployBlock


class Decisions:

    placeCard: BasePlaceCard
    trash: BaseTrash
    deployChooseCard: BaseDeployChooseCard
    deployBlock: BaseDeployBlock

    def __init__(self):
        self.placeCard = ValidatePlaceCard()
        self.trash = ValidateTrash()
        self.deployChooseCard = ValidateDeployChooseCard()
        self.deployBlock = ValidateDeployBlock()
