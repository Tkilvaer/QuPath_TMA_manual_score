/**
 * A simple script providing a manual scoring module for QuPath - some people just love to look through cores...
 */

import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.Group
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.lib.objects.TMACoreObject
import qupath.lib.regions.RegionRequest

// Define resolution - 1.0 means full size
double downsample = 1.0;

HashMap<String, String> scorMap = new HashMap<String, String>();
def scoreFactors = ["Stroma", "Tumor"];
def acceptedScores = [1,2,3,4];

//get the image servers
def server = getCurrentImageData().getServer();
def path = server.getPath();

def hierarchy = getCurrentHierarchy()
def cores = hierarchy.getTMAGrid().getTMACoreList().findAll(){it.isMissing() == false}

def showCore(TMACoreObject core, server, path, downsample){
    image = server.readBufferedImage(RegionRequest.createInstance(path, downsample, core.getROI()))
    Image img = SwingFXUtils.toFXImage(image, null)

    ImageView iv = new ImageView()
    iv.setImage(img)
    iv.setFitWidth(1000);
    iv.setPreserveRatio(true);
    iv.setSmooth(true);
    iv.setCache(true);

    Group root = new Group()
    Scene scene = new Scene(root)
    scene.setFill(Color.BLACK)
    HBox box = new HBox()
    box.getChildren().add(iv)
    root.getChildren().add(box)

    return box
}


Platform.runLater{
    def stage = new Stage()
    stage.initOwner(QuPathGUI.getInstance().getStage())
    stage.setTitle("TMA - manual score")
    def pane = new BorderPane()
    def paneBottom = new GridPane()
    int col = 0
    int row = 0
    int i = 0
    def alertScorer = new Label("Scorer is ${scorMap.get("scorer")}")
    def scorerLab = new Label("Scorer")
    def scorerText = new TextField("not set")
    scorerText.textProperty().addListener({ v, o, n ->
        try {
            scorMap.put("scorer", n)
            alertScorer.setText("Scorer is ${scorMap.get("scorer")}")
        } catch (Exception e) {}
    } as ChangeListener)

    def alertStroma = new Label()
    def stromaLab = new Label("Stroma score")
    def stromaText = new TextField()
    stromaText.textProperty().addListener({ v, o, n ->
        try {
            if(cores[i].getMeasurementList().containsNamedMeasurement("${scorMap.get("scorer")}: Stroma score")==true){
                alertStroma.setText("${scorMap.get("scorer")} has allready classfied stroma for this core")
            } else if (n.isInteger() == true) {
                if (acceptedScores.contains(Integer.parseInt(n.trim())) == true) {
                    cores[i].getMeasurementList().putMeasurement("${scorMap.get("scorer")}: Stroma score", Integer.parseInt(n.trim()))
                    alertStroma.setText("Stroma value is ${n}")
                    def names = cores[i].getMeasurementList().getMeasurementNames()
                    def scores = []
                    for (p = 0; p<names.size(); p++){
                        if (names[p].endsWith("Stroma score")==true & names[p].startsWith("Overall:")==false){
                            scores.add(cores[i].getMeasurementList().getMeasurementValue(p))
                        }
                    }
                    if (scores.size()>1){
                        cores[i].getMeasurementList().putMeasurement("Overall: Stroma score", scores.sum()/scores.size())
                    }
                    cores[i].getMeasurementList().closeList()
                } else {
                    alertStroma.setText("Stroma value ${n} not accepted")
                }
            } else {
                alertStroma.setText("Set stroma score")
            }
        } catch (Exception e) {}
    } as ChangeListener)

    def alertTumor = new Label()
    def tumorLab = new Label("Tumor score")
    def tumorText = new TextField()
    tumorText.textProperty().addListener({ v, o, n ->
        try {
            if(cores[i].getMeasurementList().containsNamedMeasurement("${scorMap.get("scorer")}: Tumor score")==true){
                alertTumor.setText("${scorMap.get("scorer")} has allready classfied tumor for this core")
            } else if (n.isInteger() == true) {
                if (acceptedScores.contains(Integer.parseInt(n.trim())) == true) {
                    print("funny stuff")
                    cores[i].getMeasurementList().putMeasurement("${scorMap.get("scorer")}: Tumor score", Integer.parseInt(n.trim()))
                    alertTumor.setText("Tumor value is ${n}")
                    def names = cores[i].getMeasurementList().getMeasurementNames()
                    def scores = []
                    for (p = 0; p<names.size(); p++){
                        if (names[p].endsWith("Tumor score")==true & names[p].startsWith("Overall:")==false){
                            scores.add(cores[i].getMeasurementList().getMeasurementValue(p))
                            print(scores.size())
                        }
                    }
                    if (scores.size()>1){
                        cores[i].getMeasurementList().putMeasurement("Overall: Tumor score", scores.sum()/scores.size())
                    }
                    cores[i].getMeasurementList().closeList()
                } else {
                    alertTumor.setText("Tumor value ${n} is not accepted")
                }
            } else {
                alertTumor.setText("Set tumor score")
            }
        } catch (Exception e) {}
    } as ChangeListener)


    def btnNext = new Button("Go to next core")
    btnNext.setOnAction {e ->
        try {
            i++
            pane.setCenter(showCore(cores[i], server, path, downsample))
            stromaText.clear()
            tumorText.clear()
            stromaText.requestFocus()
        } catch (Exception e2) {
            e2.printStackTrace()
        }
    }
    def btnBack = new Button("Go to previous core")
    btnBack.setOnAction {e ->
        try {
            i--
            pane.setCenter(showCore(cores[i], server, path, downsample))
            stromaText.clear()
            tumorText.clear()
            stromaText.requestFocus()
        } catch (Exception e2) {
            e2.printStackTrace()
        }
    }
    def btnMissing = new Button("Set core missing")
    btnMissing.setOnAction {e ->
        try {
            cores[i].setMissing(true)
            i++
            pane.setCenter(showCore(cores[i], server, path, downsample))
            stromaText.clear()
            tumorText.clear()
            stromaText.requestFocus()
        } catch (Exception e2) {
            e2.printStackTrace()
        }
    }
    def btnReset = new Button("Reset current core for scorer")
    btnReset.setOnAction {e ->
        try {
            cores[i].getMeasurementList().removeMeasurements("${scorMap.get("scorer")}: Tumor score", "${scorMap.get("scorer")}: Stroma score")
            def names = cores[i].getMeasurementList().getMeasurementNames()
            def scoresTumor = []
            def scoresStroma = []
            for (p = 0; p<names.size(); p++){
                if (names[p].endsWith("Tumor score")==true & names[p].startsWith("Overall:")==false){
                    scoresTumor.add(cores[i].getMeasurementList().getMeasurementValue(p))
                }else if (names[p].endsWith("Stroma score")==true & names[p].startsWith("Overall:")==false) {
                    scoresStroma.add(cores[i].getMeasurementList().getMeasurementValue(p))
                }
            }
            if (scoresTumor.size()>1){
                cores[i].getMeasurementList().putMeasurement("Overall: Tumor score", scoresTumor.sum()/scoresTumor.size())
            }else if (scoresStroma.size==1){
                cores[i].getMeasurementList().removeMeasurements("Overall: Tumor score")
            }
            if (scoresStroma.size()>1){
                cores[i].getMeasurementList().putMeasurement("Overall: Stroma score", scoresStroma.sum()/scoresStroma.size())
            }else if (scoresStroma.size==1) {
                cores[i].getMeasurementList().removeMeasurements("Overall: Stroma score")
            }
            cores[i].getMeasurementList().closeList()
        } catch (Exception e2) {
            e2.printStackTrace()
        }
    }
    def btnResetAll = new Button("Reset all cores for scorer")
    btnResetAll.setOnAction {e ->
        try {
            for (core in cores){
                core.getMeasurementList().removeMeasurements("${scorMap.get("scorer")}: Tumor score", "${scorMap.get("scorer")}: Stroma score")
                def names = core.getMeasurementList().getMeasurementNames()
                def scoresTumor = []
                def scoresStroma = []
                for (p = 0; p<names.size(); p++){
                    if (names[p].endsWith("Tumor score")==true & names[p].startsWith("Overall:")==false){
                            scoresTumor.add(core.getMeasurementList().getMeasurementValue(p))
                    }else if (names[p].endsWith("Stroma score")==true & names[p].startsWith("Overall:")==false) {
                        scoresStroma.add(core.getMeasurementList().getMeasurementValue(p))
                    }
                }
                if (scoresTumor.size()>1){
                    core.getMeasurementList().putMeasurement("Overall: Tumor score", scoresTumor.sum()/scoresTumor.size())
                }else if (scoresStroma.size==1){
                    core.getMeasurementList().removeMeasurements("Overall: Tumor score")
                }
                if (scoresStroma.size()>1){
                    core.getMeasurementList().putMeasurement("Overall: Stroma score", scoresStroma.sum()/scoresStroma.size())
                }else if (scoresStroma.size==1) {
                    core.getMeasurementList().removeMeasurements("Overall: Stroma score")
                }
                core.getMeasurementList().closeList()
            }
        } catch (Exception e2) {
            e2.printStackTrace()
        }
    }
    paneBottom.add(scorerLab, col, row, 1, 1)
    paneBottom.add(scorerText, col+1, row++, 1, 1)
    paneBottom.add(stromaLab, col, row, 1, 1)
    paneBottom.add(stromaText, col+1, row++, 1, 1)
    paneBottom.add(tumorLab, col, row, 1, 1)
    paneBottom.add(tumorText, col+1, row++, 1, 1)
    btnNext.setMaxWidth(Double.MAX_VALUE)
    paneBottom.add(btnNext, col, row++, 2, 1)
    btnBack.setMaxWidth(Double.MAX_VALUE)
    paneBottom.add(btnBack, col, row++, 2, 1)
    btnReset.setMaxWidth(Double.MAX_VALUE)
    paneBottom.add(btnReset, col, row++, 2, 1)
    btnMissing.setMaxWidth(Double.MAX_VALUE)
    paneBottom.add(btnMissing, col, row++, 2, 1)
    paneBottom.add(alertScorer, col, row++, 2, 1)
    paneBottom.add(alertStroma, col, row++, 2, 1)
    paneBottom.add(alertTumor, col, row++, 2, 1)
    btnResetAll.setMaxWidth(Double.MAX_VALUE)
    paneBottom.add(btnResetAll, col, row++, 2, 1)
    paneBottom.setPadding(new Insets(10))
    paneBottom.setVgap(5)
    pane.setRight(paneBottom)
    pane.setCenter(showCore(cores[i], server, path, downsample))
    Scene scene = new Scene(pane)
    stage.setScene(scene)
    stage.setWidth(1300)
    stage.setHeight(1050)
    stage.show()
}