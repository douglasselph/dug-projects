package com.kreash.devblog

import com.badlogic.gdx.Game
import com.badlogic.gdx.Screen
import com.kreash.devblog.common.di.ObjGraphCommon
import com.kreash.devblog.common.observable.MvcControl
import com.kreash.devblog.screens.di.ObjGraphScreens
import com.kreash.devblog.screens.main.MainMvcControl
import com.kreash.devblog.screens.nav.ScreenNav

/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.  */
class MainOrchestrator : Game(), ScreenNav.Hook {

    private val objGraphCommon: ObjGraphCommon by lazy { ObjGraphCommon() }
    private val objGraph: ObjGraphScreens by lazy { ObjGraphScreens(objGraphCommon) }
    private val screenNav: ScreenNav by lazy { objGraph.screenNav }
    private val mainMvcControl: MvcControl by lazy { objGraph.mainMvcControl }
    private val preferencesMvcControl: MvcControl by lazy { objGraph.preferencesMvcControl }
    private val menuMvcControl: MvcControl by lazy { objGraph.menuMvcControl }

    // region Game

    override fun create() {

        screenNav.installHook(this)
        screenNav.navigateTo(ScreenNav.Destination.MENU)

//        skin = Skin(Gdx.files.internal("ui/uiskin.json"))
//
//        val window = Window("Example screen", skin, "border")
//        window.defaults().pad(4f)
//        window.add("This is a simple Scene2D view.").row()
//        val button = TextButton("Click me!", skin)
//        button.pad(8f)
//        button.addListener(object : ChangeListener() {
//            override fun changed(event: ChangeEvent, actor: Actor) {
//                button.setText("Clicked.")
//            }
//        })
//        window.add(button)
//        window.pack()
//        window.setPosition(
//            stage.width / 2f - window.width / 2f,
//            stage.height / 2f - window.height / 2f
//        )
//        window.addAction(Actions.sequence(Actions.alpha(0f), Actions.fadeIn(1f)))
//        stage.addActor(window)
//        Gdx.input.inputProcessor = stage
    }

    override fun render() {
        super.render()
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun dispose() {
    }

    // endregion Game

    // region ScreenNav.Hook

    override fun toScreen(dest: ScreenNav.Destination) {
        setScreen(screenOf(dest))
    }

    // endregion ScreenNav.Hook


    private fun screenOf(dest: ScreenNav.Destination): Screen {
        return when (dest) {
            ScreenNav.Destination.MENU -> menuMvcControl.screen
            ScreenNav.Destination.MAIN -> mainMvcControl.screen
            ScreenNav.Destination.PREFERENCES -> preferencesMvcControl.screen
        }
    }

}