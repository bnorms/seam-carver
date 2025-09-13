import java.util.ArrayList;
import tester.*;
import javalib.impworld.*;
import javalib.worldimages.*;
import java.awt.Color;

/*
 * Notes:
 * In some of our methods (such as render, renderEnergy, leastEnergySeam, 
 * and makeFirstRowAsSeamInfo), we use casting to cast an APixel to a Pixel. 
 * This is justified because our constructPixelGraphCorner method 
 * ensures that only Pixels, and not PixelSentinels, fill the inside of the graph. 
 * PixelSentinels are only used as the boundaries of the graph. In methods such 
 * as render(), renderEnergy(), and makeFirstRowAsSeamInfo(), we begin traversal
 * of the graph at corner.down.right, and only continue to traverse through interior pixels. 
 * This guarantees that each visited node is a Pixel and not a PixelSentinel. 
 * We also have a method called isWellFormed() that ensures that our structural
 * invariant remains valid.
 * 
 */

//convenient computations and methods that don't fit into other classes
class Utils {

  // Helper for construction of the corner of the pixel graph
  // EFFECT: mutates the corner by adding pixels to it, forming a representation of an image
  void constructPixelGraphCorner(PixelSentinel corner,
      int width, int height, FromFileImage image) {

    corner.expandHorizontally(width);

    for (int y = 0; y < height; y += 1) {
      PixelSentinel rowPixel = new PixelSentinel(corner, "vertical");
      APixel columnPixel = rowPixel;
      // EFFECT: modifies the references of the new and old pixels based on where
      // the new one is added
      for (int x = width - 1; x >= 0; x -= 1) {
        Pixel newPixel = new Pixel(image.getColorAt(x, y));
        newPixel.updateRight(columnPixel);
        newPixel.updateUp(columnPixel.up.left);
        columnPixel.updateLeft(newPixel);
        columnPixel.up.left.updateDown(newPixel);
        if (x == 0) {
          // EFFECT: if a pixel is placed at the left edge, its left
          // link is modified to be a sentinel and the same sentinel's
          // right link is modified to be the pixel
          newPixel.updateLeft(rowPixel);
          rowPixel.updateRight(newPixel);
        }
        if (y == height - 1) {
          // EFFECT: if a pixel is placed at the bottom bottom edge,
          // its bottom link is modified to be a sentinel and the
          // same sentinel's upper link is modified to be the pixel
          newPixel.updateDown(columnPixel.down.left);
          columnPixel.down.left.updateUp(newPixel);
        }
        columnPixel = newPixel;
      }
    }
  }

  // Determines the minimum weight SeamInfo in the given ArrayList
  SeamInfo minInfo(ArrayList<SeamInfo> result) {
    SeamInfo finalResult;
    if (result.size() > 0) {
      finalResult = result.get(0);
    }
    else {
      finalResult = null;
    }
    // EFFECT: modifies finalResult to be the seam info of smallest weight in the list
    for (int length = 1; length < result.size(); length += 1) {
      if (result.get(length).totalWeight < finalResult.totalWeight) {
        finalResult = result.get(length);
      }
    }
    return finalResult;
  }
}

// Represents a pixel
abstract class APixel {
  APixel right;
  APixel left;
  APixel up;
  APixel down;

  // Constructor
  APixel(APixel right, APixel left, APixel up, APixel down) {
    this.right = right;
    this.left = left;
    this.up = up;
    this.down = down;
  }

  // Modifies the this pixel's right pixel to be some given pixel
  void updateRight(APixel p) {
    this.right = p;
  }

  // Modifies the this pixel's left pixel to be some given pixel
  void updateLeft(APixel p) {
    this.left = p;
  }

  // Modifies the this pixel's upper pixel to be some given pixel
  void updateUp(APixel p) {
    this.up = p;
  }

  //Modifies the this pixel's lower pixel to be some given pixel
  void updateDown(APixel p) {
    this.down = p;
  }

  // Modifies the down or right pixel to be some given pixel based on the given
  // boolean indicating direction (which is either vertical or horizontal)
  void updatePositiveDirection(APixel p, boolean movingVertical) {
    if (movingVertical) {
      this.updateDown(p);
    }
    else {
      this.updateRight(p);
    }
  }

  // Modifies the up or left pixel to be some given pixel based on the given
  // boolean indicating direction  (which is either vertical or horizontal)
  void updateNegativeDirection(APixel p, boolean movingVertical) {
    if (movingVertical) {
      this.updateUp(p);
    }
    else {
      this.updateLeft(p);
    }
  }

  // Produces the pixel either down or right from this one based on the given boolean
  APixel moveInPositiveDirection(boolean movingVertical) {
    if (movingVertical) {
      return this.down;
    }
    return this.right;
  }

  // Produces the pixel either up or left from this one based on the given boolean
  APixel moveInNegativeDirection(boolean movingVertical) {
    if (movingVertical) {
      return this.up;
    }
    return this.left;
  }

  // Removes this pixel assuming a pixel directly adjacent (down if vertical or right
  // if horizontal) will be removed too
  // EFFECT: modifies this pixel's neighbors to bypass this pixel adjacent neighbor
  void removeThisPixelAligned(boolean movingVertical) {
    this.moveInNegativeDirection(!movingVertical)
        .updatePositiveDirection(this.moveInPositiveDirection(!movingVertical), !movingVertical);
    this.moveInPositiveDirection(!movingVertical)
        .updateNegativeDirection(this.moveInNegativeDirection(!movingVertical), !movingVertical);
  }

  // Removes this pixel assuming a pixel along the negative diagonal (down-left if vertical or
  // right-up if horizontal) will be removed too
  // EFFECT: modifies this pixel's neighbors to bypass this pixel and its negative diagonal neighbor
  void removeThisPixelDiagonalNegative(boolean movingVertical) {
    this.removeThisPixelAligned(movingVertical);
    this.moveInNegativeDirection(movingVertical)
        .updatePositiveDirection(this.moveInNegativeDirection(!movingVertical), movingVertical);
    this.moveInNegativeDirection(!movingVertical)
        .updateNegativeDirection(this.moveInNegativeDirection(movingVertical), movingVertical);
  }

  // removes this pixel assuming a pixel along the positive diagonal (down-right if vertical or 
  // right-down if horizontal) will be removed too
  // EFFECCT: modifies this pixel's neighbors to bypass this pixel and its 
  // positive diagonal neighbor
  void removeThisPixelDiagonalPositive(boolean movingVertical) {
    this.removeThisPixelAligned(movingVertical);
    this.moveInNegativeDirection(movingVertical)
        .updatePositiveDirection(this.moveInPositiveDirection(!movingVertical), movingVertical);
    this.moveInPositiveDirection(!movingVertical)
        .updateNegativeDirection(this.moveInNegativeDirection(movingVertical), movingVertical);
  }

  // Calculates the brightness
  abstract double calculateBrightness();

  // Determines whether this pixel's neighbors point to it
  boolean neighborsValid() {
    return (this.left.right == this) && (this.right.left == this) && (this.up.down == this)
        && (this.down.up == this);
  }
}

// Represents a pixel of a certain color
class Pixel extends APixel {
  // Fields
  Color color;

  // Constructor
  Pixel(APixel right, APixel left, APixel up, APixel down, Color color) {
    super(right, left, up, down);
    this.color = color;
    this.right.updateLeft(this);
    this.left.updateRight(this);
    this.up.updateDown(this);
    this.down.updateUp(this);
  }

  // Null Constructor for Color
  Pixel(Color color) {
    super(null, null, null, null);
    this.color = color;
  }

  // Calculate's this pixel's brightness
  double calculateBrightness() {
    return ((this.color.getRed() + this.color.getBlue() + this.color.getGreen()) / 3) / 255.0;
  }

  // Calculate's this pixel's horizontal energy
  double calculateHorizontalEnergy() {
    return (this.up.left.calculateBrightness() + 2.0 * this.left.calculateBrightness()
        + this.down.left.calculateBrightness())
        - (this.up.right.calculateBrightness() + 2.0 * this.right.calculateBrightness()
        + this.down.right.calculateBrightness());
  }

  // Calculate's this pixel's vertical energy
  double calculateVerticalEnergy() {
    return (this.up.left.calculateBrightness() + 2.0 * this.up.calculateBrightness()
        + this.up.right.calculateBrightness())
        - (this.down.left.calculateBrightness() + 2.0 * this.down.calculateBrightness()
        + this.down.right.calculateBrightness());
  }

  // Calculate's this pixel's energy
  double calculateEnergy() {
    return Math.sqrt(Math.pow(this.calculateHorizontalEnergy(), 2.0)
        + Math.pow(this.calculateVerticalEnergy(), 2.0));
  }
}

// Represents a sentinel guarding the edges of Pixels
class PixelSentinel extends APixel {
  // Constructor
  PixelSentinel() {
    super(null, null, null, null);
    this.updateRight(this);
    this.updateLeft(this);
    this.updateUp(this);
    this.updateDown(this);
  }

  // Constructor to add to a given sentinel in the given direction (vertically or
  // horizontally)
  PixelSentinel(PixelSentinel s, String direction) {
    super(null, null, null, null);
    if (direction.equals("horizontal")) {
      this.updateLeft(s.left);
      this.updateRight(s);
      this.updateUp(this);
      this.updateDown(this);

      s.updateLeft(this);
      s.left.left.updateRight(this);
    }
    else if (direction.equals("vertical")) {
      this.updateUp(s.up);
      this.updateDown(s);
      this.updateLeft(this);
      this.updateRight(this);

      s.updateUp(this);
      s.up.up.updateDown(this);
    }
    else {
      throw new IllegalArgumentException("invalid direction");
    }
  }

  // Expands this sentinel horizontally by the given number
  void expandHorizontally(int toAdd) {
    for (int remaining = toAdd; remaining > 0; remaining -= 1) {
      new PixelSentinel(this, "horizontal");
    }
  }

  // Calculates the brightness of this
  double calculateBrightness() {
    return 0.0;
  }
}

// Represents a rectangular graph of pixels
class PixelGraph {
  PixelSentinel corner;
  int width;
  int height;

  // Convenience Constructor with new Sentinel
  PixelGraph() {
    this.corner = new PixelSentinel();
    this.width = 0;
    this.height = 0;
  }

  // Constructor
  PixelGraph(PixelSentinel corner, int width, int height) {
    this.corner = corner;
    this.width = width;
    this.height = height;
  }

  // Image Constructor
  PixelGraph(FromFileImage image) {
    this.corner = new PixelSentinel();
    this.width = (int) image.getWidth();
    this.height = (int) image.getHeight();
    new Utils().constructPixelGraphCorner(corner, width, height, image);
  }

  // Renders this PixelGraph according to the colors of its pixels
  ComputedPixelImage render() {
    ComputedPixelImage result = new ComputedPixelImage(this.width, this.height);
    APixel rowPixel = this.corner.down;
    // Loop: Iterates over each row of the pixel graph from top to bottom
    for (int y = 0; y < this.height; y += 1) {
      APixel columnPixel = rowPixel.right;
      // Loop: Iterates over each pixel in the current row from left to right
      for (int x = 0; x < this.width; x += 1) {
        // EFFECT: sets each pixel in the current row of result
        // to match the color of the corresponding pixel in the graph
        result.setPixel(x, y, (((Pixel) columnPixel).color));
        columnPixel = columnPixel.right;
      }
      rowPixel = rowPixel.down;
    }
    return result;
  }

  // Renders this PixelGraph according to the energy of its pixels
  ComputedPixelImage renderEnergy() {
    ComputedPixelImage result = new ComputedPixelImage(this.width, this.height);
    APixel rowPixel = this.corner.down;
    for (int y = 0; y < this.height; y += 1) {
      APixel columnPixel = rowPixel.right;
      // Loop: Iterates over each pixel in the current row from left to right
      for (int x = 0; x < this.width; x += 1) {
        int colorChannel = (int) ((((Pixel) columnPixel).calculateEnergy() / 6) * 255);
        Color energyColor = new Color(colorChannel, colorChannel, colorChannel);
        // EFFECT: sets each pixel in the current row of the result image to a  
        // color representing the energy of the corresponding pixel in the graph
        result.setPixel(x, y, energyColor);
        columnPixel = columnPixel.right;
      }
      rowPixel = rowPixel.down;
    }
    return result;
  }

  // Finds the least energy seam in this graph in the direction corresponding to the given boolean
  SeamInfo leastEnergySeam(boolean vertical) {
    ArrayList<SeamInfo> result = this.makeFirstRowAsSeamInfo(vertical);
    int length1;
    int length2;
    // Determines what direction to traverse in based on direction
    if (vertical) {
      length1 = this.height;
      length2 = this.width;
    }
    else {
      length1 = this.width;
      length2 = this.height;
    }

    // rowPixel is set to either the second row or column based on the direction
    APixel rowPixel = this.corner.moveInPositiveDirection(vertical)
        .moveInPositiveDirection(vertical);

    // EFFECT: Iteratively construct SeamInfo for each row/column depending
    // the given direction
    for (int edgeIndex = 1; edgeIndex < length1; edgeIndex += 1) {
      APixel columnPixel = rowPixel.moveInPositiveDirection(!vertical);
      ArrayList<SeamInfo> tempResult = result;
      result = new ArrayList<SeamInfo>();

      // EFFECT: Mutates result to contain the current row's seam info
      for (int innerIndex = 0; innerIndex < length2; innerIndex += 1) {
        int x;
        int y;

        if (vertical) {
          x = innerIndex;
          y = edgeIndex;
        }
        else {
          x = edgeIndex;
          y = innerIndex;
        }
        if (innerIndex == 0 && innerIndex == length2 - 1) {
          // EFFECT: modifies result to contain a new SeamInfo
          result.add(innerIndex,
              new SeamInfo((Pixel) columnPixel, tempResult.get(innerIndex), x, y));
        }
        else if (innerIndex == 0) {
          // EFFECT: modifies result to contain a new SeamInfo based on the lower
          // energy between two possible paths
          result.add(innerIndex, new SeamInfo((Pixel) columnPixel,
              tempResult.get(innerIndex).lowerEnergy(tempResult.get(innerIndex + 1)), x, y));
        }
        else if (innerIndex == length2 - 1) {
          // EFFECT: modifies result to contain a new SeamInfo based on the lower
          // energy between two possible paths
          result.add(innerIndex, new SeamInfo((Pixel) columnPixel,
              tempResult.get(innerIndex - 1).lowerEnergy(tempResult.get(innerIndex)), x, y));
        }
        else {
          // EFFECT: modifies result to contain a new SeamInfo based on which of three 
          // seams has the lowest energy
          result.add(innerIndex,
              new SeamInfo((Pixel) columnPixel,
                  tempResult.get(innerIndex - 1).lowerEnergy(
                      tempResult.get(innerIndex).lowerEnergy(tempResult.get(innerIndex + 1))),
                  x, y));

        }
        columnPixel = columnPixel.moveInPositiveDirection(!vertical);
      }
      rowPixel = rowPixel.moveInPositiveDirection(vertical);
    }
    return new Utils().minInfo(result);
  }

  // Creates a list of SeamInfo objects representing the first row (if vertical) 
  //or first column (if horizontal) of the pixel graph 
  ArrayList<SeamInfo> makeFirstRowAsSeamInfo(boolean vertical) {
    ArrayList<SeamInfo> result = new ArrayList<SeamInfo>();
    APixel firstRowPixel = this.corner.down.right;
    int length;
    if (vertical) {
      length = this.width;
    }
    else {
      length = this.height;
    }
    // EFFECT: Iterates through the first row or column and adds
    // each pixel's SeamInfo to result
    for (int x = 0; x < length; x += 1) {
      if (vertical) {
        result.add(new SeamInfo((Pixel) firstRowPixel, null, x, 0));
      }
      else {
        result.add(new SeamInfo((Pixel) firstRowPixel, null, 0, x));
      }
      firstRowPixel = firstRowPixel.moveInPositiveDirection(!vertical);
    }
    return result;
  }

  // Removes the given seam from this pixelGraph given the direction of the seam
  // EFFECT: removes the given seam from this pixelGraph and updates the width accordingly
  // and pixel references accordingly
  void removeSeam(SeamInfo seam, boolean vertical) {
    seam.deleteThisSeam(vertical);
    APixel curr;
    APixel negativeSide;
    APixel positiveSide;
    int length;
    if (vertical) {
      // EFFECT: decreases the graph's width to account for the removed seam
      this.width -= 1;
      length = this.width;
    }
    else {
      // EFFECT: decreases the graph's height to account for the removed seam
      this.height -= 1;
      length = this.height;
    }

    // EFFECT: updates the sentinel references to skip over the removed seam's edge
    this.corner.updatePositiveDirection(
        this.corner.moveInPositiveDirection(!vertical).moveInPositiveDirection(!vertical), 
        !vertical);
    this.corner.moveInPositiveDirection(!vertical).updateNegativeDirection(this.corner, !vertical);
    curr = this.corner.moveInPositiveDirection(!vertical);
    negativeSide = this.corner.moveInNegativeDirection(vertical).moveInPositiveDirection(!vertical);
    positiveSide = this.corner.moveInPositiveDirection(vertical).moveInPositiveDirection(!vertical);

    // EFFECT: Mutates this corner to fix the reference's of its horizontal
    // sentinels
    for (int x = 0; x < length; x += 1) {
      curr.updateNegativeDirection(negativeSide, vertical);
      negativeSide.updatePositiveDirection(curr, vertical);
      curr.updatePositiveDirection(positiveSide, vertical);
      positiveSide.updateNegativeDirection(curr, vertical);

      // EFFECT: sets curr, negativeSide, and positiveSide to be the next pixel
      // in the row or column based on the given direction
      curr = curr.moveInPositiveDirection(!vertical);
      negativeSide = negativeSide.moveInPositiveDirection(!vertical);
      positiveSide = positiveSide.moveInPositiveDirection(!vertical);
    }
  }

  // Turns the pixels in the given seam red
  void makeSeamRed(SeamInfo seam) {
    seam.showSeam();
  }

  // Renders this PixelGraph according to the colors of its pixels
  boolean isWellFormed() {
    if (!this.corner.neighborsValid()) {
      return false;
    }
    APixel rowPixel = this.corner.down;
    // Loop: Iterates through each row of this pixel graph from top to bottom
    for (int y = 0; y < this.height; y += 1) {
      APixel columnPixel = rowPixel.right;
      // Loop : Iterates through each pixel in the current row from left to right
      for (int x = 0; x < this.width; x += 1) {
        if (!columnPixel.neighborsValid()) {
          return false;
        }
        columnPixel = columnPixel.right;
      }
      rowPixel = rowPixel.down;
    }
    return true;
  }

}

// Represents the info of a seam
class SeamInfo {
  Pixel curr;
  double totalWeight;
  SeamInfo cameFrom;
  int x;
  int y;

  // Constructor
  SeamInfo(Pixel curr, SeamInfo cameFrom, int x, int y) {
    this.curr = curr;
    this.cameFrom = cameFrom;
    this.x = x;
    this.y = y;

    if (cameFrom == null) {
      this.totalWeight = curr.calculateEnergy();
    }
    else {
      this.totalWeight = curr.calculateEnergy() + this.cameFrom.totalWeight;
    }
  }

  // Returns this seam if it has a lower energy than that seam, and returns that otherwise
  SeamInfo lowerEnergy(SeamInfo that) {
    if (that.totalWeight < this.totalWeight) {
      return that;
    }
    return this;
  }

  // Removes every pixel in this seam from the pixel graph
  // EFFECT: Modifies pixel graph by modifying the pixels in the SeamInfo's
  // neighbors to preserve the structural invariant
  void deleteThisSeam(boolean vertical) {
    if (cameFrom != null) {
      int position;
      if (vertical) {
        position = this.x;
      }
      else {
        position = this.y;
      }
      if (position == this.cameFrom.calculatePosition(vertical)) {
        // EFFECT: Removes this pixel and modifies its neighbors accordingly, 
        // assuming that a pixel adjacent to this (based on the given direction)
        // will be removed too
        curr.removeThisPixelAligned(vertical);
      }
      else if ((position - 1) == this.cameFrom.calculatePosition(vertical)) {
        // EFFECT: Removes this pixel and modifies its neighbors accordingly, 
        // assuming that a pixel at its negative diagonal (based on the given direction)
        // will be removed too
        curr.removeThisPixelDiagonalNegative(vertical);
      }
      else if ((position + 1) == this.cameFrom.calculatePosition(vertical)) {
        // EFFECT: Removes this pixel and modifies its neighbors accordingly, 
        // assuming that a pixel at its positive diagonal (based on the given direction)
        // will be removed too
        curr.removeThisPixelDiagonalPositive(vertical);
      }
      // EFFECT: removes the remaining pixels in the seam
      this.cameFrom.deleteThisSeam(vertical);
    }
    else {
      // EFFECT: if there is no prior pixel, removes the first pixel in the seam
      curr.removeThisPixelAligned(vertical);
    }
  }

  // Turns each pixel in this seam red
  // EFFECT: mutates each pixel in this seam to have its color field be red
  void showSeam() {
    this.curr.color = Color.RED;
    if (this.cameFrom != null) {
      this.cameFrom.showSeam();
    }
  }

  // Determines the relevant coordinate (x or y) depending on the seam direction given
  int calculatePosition(boolean vertical) {
    if (vertical) {
      return this.x;
    }
    return this.y;
  }

}

// Represents a world holding a graph representing an image being carved
class CarvingWorld extends World {
  PixelGraph graph;
  int tickCounter = 0;
  SeamInfo currSeam;
  boolean energyToggle;
  boolean vertical;
  boolean paused;

  // Constructor
  CarvingWorld(PixelGraph graph) {
    this.graph = graph;
    this.energyToggle = false;
    this.vertical = true;
    this.paused = false;
  }

  // Colors then deletes the lowest energy seam in this world
  // EFFECT: changes the color of pixels in the lowest energy seam or removes those pixels
  public void onTick() {
    if ((tickCounter % 2) == 0) {
      if (!paused) {
        //this.vertical = Math.random() < 0.5;
        currSeam = this.graph.leastEnergySeam(vertical);
        this.graph.makeSeamRed(currSeam);
        tickCounter = tickCounter + 1;
      }
    }
    else {
      this.graph.removeSeam(currSeam, vertical);
      tickCounter = tickCounter + 1;
    }
    if ((this.graph.width == 1) || (this.graph.height == 1)) {
      this.endOfWorld("Image gone");
    }
  }

  // Changes the world based on key events
  // EFFECT: toggles the energy toggle field to change rendering behavior
  public void onKeyEvent(String key) {
    if (key.equals("e")) {

      this.energyToggle = !this.energyToggle;
    }
    else if (key.equals(" ")) {
      this.paused = !this.paused;
    }
    else if (key.equals("v") && paused) {
      this.graph.removeSeam(this.graph.leastEnergySeam(true), true);
    }
    else if (key.equals("h") && paused) {
      this.graph.removeSeam(this.graph.leastEnergySeam(false), false);
    }
  }

  // Renders this scene
  public WorldScene makeScene() {
    ComputedPixelImage image;
    if (energyToggle) {
      image = this.graph.renderEnergy();
    }
    else {
      image = this.graph.render();
    }
    WorldScene levelScene = new WorldScene(this.graph.width, this.graph.height);
    levelScene.placeImageXY(image, this.graph.width / 2, this.graph.height / 2);
    return levelScene;
  }

  // Renders the last scene
  public WorldScene lastScene() {
    WorldScene levelScene = new WorldScene(1, this.graph.height);
    return levelScene;
  }

}

class ExamplesSeamCarving {
  /*
   * TODO:
   * - leastEnergySeam for horizontal
   * - makeFirstRowAsSeam for horizontal
   * - removeSeam for horizontal
   * - deleteThisSeam for horizontal
   * - renderEnergy
   * - add more tests to calculatePosition?
   */
  PixelGraph graph1;

  PixelSentinel pixelcorner;
  PixelSentinel pixelR0;
  PixelSentinel pixelR1;
  PixelSentinel pixelR2;
  PixelSentinel pixelD0;
  PixelSentinel pixelD1;
  PixelSentinel pixelD2;

  Pixel pixel00;
  Pixel pixel01;
  Pixel pixel02;
  Pixel pixel10;
  Pixel pixel11;
  Pixel pixel12;
  Pixel pixel20;
  Pixel pixel21;
  Pixel pixel22;

  void initData() {

    // Sentinels
    this.pixelcorner = new PixelSentinel();
    this.pixelR0 = new PixelSentinel(pixelcorner, "horizontal");
    this.pixelR1 = new PixelSentinel(pixelR0, "horizontal");
    this.pixelR2 = new PixelSentinel(pixelR1, "horizontal");

    this.pixelD2 = new PixelSentinel(pixelcorner, "vertical");
    this.pixelD1 = new PixelSentinel(pixelcorner, "vertical");
    this.pixelD0 = new PixelSentinel(pixelcorner, "vertical");

    // Pixels
    this.pixel00 = new Pixel(new Color(0, 0, 0));
    this.pixel01 = new Pixel(new Color(255, 255, 255));
    this.pixel02 = new Pixel(new Color(0, 0, 0));

    this.pixel10 = new Pixel(new Color(255, 0, 0));           
    this.pixel11 = new Pixel(new Color(0, 255, 0));         
    this.pixel12 = new Pixel(new Color(0, 0, 255));

    this.pixel20 = new Pixel(new Color(255, 255, 0));     
    this.pixel21 = new Pixel(new Color(0, 255, 255));         
    this.pixel22 = new Pixel(new Color(255, 0, 255));   

    this.pixel00 = new Pixel(pixel01, pixelD0, pixelR0, pixel10, new Color(0, 0, 0)); 
    this.pixel01 = new Pixel(pixel02, pixel00, pixelR1, pixel11, new Color(255, 255, 255)); 
    this.pixel02 = new Pixel(pixelD0, pixel01, pixelR2, pixel12, new Color(0, 0, 0));

    this.pixel10 = new Pixel(pixel11, pixelD1, pixel00, pixel20, new Color(255, 0, 0));           
    this.pixel11 = new Pixel(pixel12, pixel10, pixel01, pixel21, new Color(0, 255, 0));         
    this.pixel12 = new Pixel(pixelD1, pixel11, pixel02, pixel22, new Color(0, 0, 255));

    this.pixel20 = new Pixel(pixel21, pixelD2, pixel10, pixelR0, new Color(255, 255, 0));     
    this.pixel21 = new Pixel(pixel22, pixel20, pixel11, pixelR1, new Color(0, 255, 255));         
    this.pixel22 = new Pixel(pixelD2, pixel21, pixel12, pixelR2, new Color(255, 0, 255));     
  }

  void testBigBang(Tester t) {
    PixelGraph image = new PixelGraph(new FromFileImage("images/balloons.jpg"));
    CarvingWorld w = new CarvingWorld(image);
    int worldWidth = image.width;
    int worldHeight = image.height;
    double tickRate = 0.02;
    w.bigBang(worldWidth, worldHeight, tickRate);
  }

  // test removing horizontal seams
  void testHorizontalIsWellFormed(Tester t) {
    PixelGraph image = new PixelGraph(new FromFileImage("images/balloons.jpg"));
    t.checkExpect(image.isWellFormed(), true);
    image.removeSeam(image.leastEnergySeam(false), false);
    t.checkExpect(image.isWellFormed(), true);
  }

  // wellFormed()
  void testWellFormedness(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);
    PixelGraph graphEmpty = new PixelGraph(new PixelSentinel(), 0, 0);
    t.checkExpect(graph.isWellFormed(), true);

    graph.corner.right.down.updateLeft(graph.corner.right.down.right);
    
    t.checkExpect(graph.isWellFormed(), false);
    t.checkExpect(graphEmpty.isWellFormed(), true);
  }

  // updateDirection()
  void testUpdateDirection(Tester t) {
    this.initData();
    Pixel testPixel = new Pixel(Color.BLACK);

    // Test updateRight
    t.checkExpect(pixel10.right, pixel11);
    pixel10.updateRight(testPixel);
    t.checkExpect(pixel10.right, testPixel);

    t.checkExpect(pixel22.right, pixelD2);
    pixel22.updateRight(testPixel);
    t.checkExpect(pixel22.right, testPixel);

    // Test updateLeft
    t.checkExpect(pixel11.left, pixel10);
    pixelR1.updateLeft(testPixel);
    t.checkExpect(pixelR1.left, testPixel);

    t.checkExpect(pixel00.left, pixelD0);
    pixel00.updateLeft(testPixel);
    t.checkExpect(pixel00.left, testPixel);

    // Test updateUp
    t.checkExpect(pixel02.up, pixelR2);
    pixel02.updateUp(testPixel);
    t.checkExpect(pixel02.up, testPixel);

    t.checkExpect(pixel11.up, pixel01);
    pixel11.updateUp(testPixel);
    t.checkExpect(pixel11.up, testPixel);

    // Test updateDown
    t.checkExpect(pixel12.down, pixel22);
    pixel12.updateDown(testPixel);
    t.checkExpect(pixel12.down, testPixel);

    t.checkExpect(pixel21.down, pixelR1);
    pixel21.updateDown(testPixel);
    t.checkExpect(pixel12.down, testPixel);
  }

  // updatePositiveDirection() vertical
  void testUpdatePositiveDirectionVertical(Tester t) {
    this.initData();
    Pixel testPixel = new Pixel(Color.BLACK);

    t.checkExpect(this.pixel11.down, this.pixel21);
    this.pixel11.updatePositiveDirection(testPixel, true);
    t.checkExpect(this.pixel11.down, testPixel);

  }

  // updatePositiveDirection() horizontal
  void testUpdatePositiveDirectionHorizontal(Tester t) {
    this.initData();
    Pixel testPixel = new Pixel(Color.BLACK);

    t.checkExpect(this.pixel11.right, this.pixel12);
    this.pixel11.updatePositiveDirection(testPixel, false);
    t.checkExpect(this.pixel11.right, testPixel);
  }

  // updateNegativeDirection() vertical
  void testUpdateNegativeDirectionVertical(Tester t) {
    this.initData();
    Pixel testPixel = new Pixel(Color.BLACK);

    t.checkExpect(this.pixel11.up, this.pixel01);
    this.pixel11.updateNegativeDirection(testPixel, true);
    t.checkExpect(this.pixel11.up, testPixel);
  }

  // updateNegativeDirection() horizontal
  void testUpdateNegativeDirection(Tester t) {
    this.initData();
    Pixel testPixel = new Pixel(Color.BLACK);

    t.checkExpect(this.pixel11.left, this.pixel10);
    this.pixel11.updateNegativeDirection(testPixel, false);
    t.checkExpect(this.pixel11.left, testPixel);
  }

  // moveInPositiveDirection()
  void testMoveInPositiveDirection(Tester t) {
    this.initData();

    // test moveInPositiveDirection when moving in the positive vertical direction (down)
    t.checkExpect(this.pixel00.moveInPositiveDirection(true), this.pixel10);
    t.checkExpect(this.pixel10.moveInPositiveDirection(true), this.pixel20);
    t.checkExpect(this.pixel20.moveInPositiveDirection(true), this.pixelR0); 

    // test moveInPositiveDirection when moving in the positive horizontal (right)
    t.checkExpect(this.pixel00.moveInPositiveDirection(false), this.pixel01);
    t.checkExpect(this.pixel01.moveInPositiveDirection(false), this.pixel02);
    t.checkExpect(this.pixel02.moveInPositiveDirection(false), this.pixelD0); 
  }

  // moveInNegativeDirection()
  void testMoveInNegativeDirection(Tester t) {
    this.initData();

    // test moveInPositiveDirection when moving in the negative vertical direction (up)
    t.checkExpect(this.pixel20.moveInNegativeDirection(true), this.pixel10);
    t.checkExpect(this.pixel10.moveInNegativeDirection(true), this.pixel00);
    t.checkExpect(this.pixel00.moveInNegativeDirection(true), this.pixelR0); // hits sentinel at top

    // test moveInPositiveDirection when moving in the negative horizontal (left)
    t.checkExpect(this.pixel02.moveInNegativeDirection(false), this.pixel01);
    t.checkExpect(this.pixel01.moveInNegativeDirection(false), this.pixel00);
    t.checkExpect(this.pixel00.moveInNegativeDirection(false), this.pixelD0);
  }

  // removeThisPixelVertical() vertical
  void testRemoveThisPixelAlignedVertical(Tester t) {
    PixelGraph graph = new PixelGraph(new FromFileImage("images/testImage.png"));

    APixel middle = graph.corner.down.right.right;
    APixel left = middle.left;
    APixel right = middle.right;

    t.checkExpect(left.right, middle);
    t.checkExpect(right.left, middle);

    ((Pixel) middle).removeThisPixelAligned(true);

    t.checkExpect(left.right, right);
    t.checkExpect(right.left, left);
  }

  // removeThisPixelVertical() horizontal
  void testRemoveThisPixelAlignedHorizontal(Tester t) {
    PixelGraph graph = new PixelGraph(new FromFileImage("images/testImage.png"));

    APixel middle = graph.corner.down.right.right; 
    APixel up = middle.up;
    APixel down = middle.down;

    t.checkExpect(up.down, middle);
    t.checkExpect(down.up, middle);

    ((Pixel) middle).removeThisPixelAligned(false);

    t.checkExpect(up.down, down);
    t.checkExpect(down.up, up);
  }

  // removeThisPixelDiagonalNegative() vertical
  void testRemoveThisPixelDiagonalNegativeVertical(Tester t) {
    PixelGraph graph = new PixelGraph(new FromFileImage("images/testImage.png"));

    APixel middle = graph.corner.down.right.right;
    APixel up = middle.up;
    APixel left = middle.left;

    t.checkExpect(left.right, middle);
    t.checkExpect(up.down, middle);
    t.checkExpect(left.up, graph.corner.right);

    ((Pixel) middle).removeThisPixelDiagonalNegative(true);

    t.checkExpect(left.right, middle.right);
    t.checkExpect(up.down, left);
    t.checkExpect(left.up, up);
  }

  //removeThisPixelDiagonalNegative() horizontal
  void testRemoveThisPixelDiagonalNegativeHorizontal(Tester t) {
    PixelGraph graph = new PixelGraph(new FromFileImage("images/testImage.png"));
    APixel middle = graph.corner.down.right.right; 
    APixel up = middle.up;
    APixel left = middle.left;

    t.checkExpect(up.down, middle);
    t.checkExpect(middle.down.up, middle);
    t.checkExpect(left.right, middle);
    t.checkExpect(up.left, graph.corner.right);

    ((Pixel) middle).removeThisPixelDiagonalNegative(false);

    t.checkExpect(up.down, middle.down);
    t.checkExpect(middle.down.up, up);
    t.checkExpect(left.right, up);
    t.checkExpect(up.left, left);
  }


  // removeThisPixelDiagonalPositive() vertical
  void testRemoveThisPixelDiagonalPositiveVertical(Tester t) {
    PixelGraph graph = new PixelGraph(new FromFileImage("images/testImage.png"));

    Pixel pixelToRemove = (Pixel) graph.corner.down.right;          
    Pixel pixelToRemoveRight = (Pixel) graph.corner.down.right.right;          
    Pixel pixelToRemovePositiveDiagonal = (Pixel) pixelToRemove.down.right;  

    t.checkExpect(pixelToRemoveRight.left, pixelToRemove);
    t.checkExpect(pixelToRemovePositiveDiagonal.left, pixelToRemove.down);

    ((Pixel) pixelToRemove).removeThisPixelDiagonalPositive(true);

    t.checkExpect(graph.corner.down.right, pixelToRemoveRight);
    t.checkExpect(pixelToRemoveRight.down, pixelToRemovePositiveDiagonal);
  }

  // removeThisPixelDiagonalPositive() horizontal
  void testRemoveThisPixelDiagonalPositiveHorizontal(Tester t) {
    PixelGraph graph = new PixelGraph(new FromFileImage("images/testImage.png"));

    APixel middle = graph.corner.down.right.right;
    APixel up = middle.up;
    APixel down = middle.down;
    APixel right = middle.right;
    APixel left = middle.left;

    t.checkExpect(graph.corner.down.right.right, middle);
    t.checkExpect(left.right, middle);
    t.checkExpect(up.down, middle);

    ((Pixel) middle).removeThisPixelDiagonalPositive(false);

    t.checkExpect(graph.corner.down.right.right, down);
    t.checkExpect(left.right, down);
    t.checkExpect(up.down, down);
  }

  // calculateBrightness()
  void testCalculateBrightness(Tester t) {
    this.initData();
    t.checkInexact(this.pixelR0.calculateBrightness(), 0.0, .1);
    t.checkInexact(this.pixelD2.calculateBrightness(), 0.0, .1);
    t.checkInexact(this.pixel00.calculateBrightness(), 0.0, .1);
    t.checkInexact(this.pixel01.calculateBrightness(), 1.0, .1);
    t.checkInexact(this.pixel11.calculateBrightness(), .33, .1);
    t.checkInexact(this.pixel12.calculateBrightness(), .33, .1);
    t.checkInexact(this.pixel20.calculateBrightness(), .66, .1);
    t.checkInexact(this.pixel21.calculateBrightness(), .66, .1);
  }

  // calculateHorizontalEnergy()
  void testCalculateHorizontalEnergy(Tester t) {
    this.initData();
    t.checkExpect(pixel00.calculateHorizontalEnergy(),
        (this.pixelcorner.calculateBrightness() + (2 * this.pixelD0.calculateBrightness())
            + this.pixelD1.calculateBrightness())
        - (this.pixelR1.calculateBrightness() + (2 * this.pixel01.calculateBrightness())
            + this.pixel11.calculateBrightness()));
    t.checkExpect(pixel11.calculateHorizontalEnergy(),
        (this.pixel00.calculateBrightness() + (2 * this.pixel10.calculateBrightness())
            + this.pixel20.calculateBrightness())
        - (this.pixel02.calculateBrightness() + (2 * this.pixel12.calculateBrightness())
            + this.pixel22.calculateBrightness()));
    t.checkExpect(pixel21.calculateHorizontalEnergy(),
        (this.pixel10.calculateBrightness() + (2 * this.pixel20.calculateBrightness())
            + this.pixelR0.calculateBrightness())
        - (this.pixel12.calculateBrightness() + (2 * this.pixel22.calculateBrightness())
            + this.pixelR2.calculateBrightness()));
  }

  // calculateVerticalEnergy()
  void testCalculateVerticalEnergy(Tester t) {
    this.initData();
    t.checkExpect(pixel00.calculateVerticalEnergy(),
        (this.pixelcorner.calculateBrightness() + (2 * this.pixelR0.calculateBrightness())
            + this.pixelR1.calculateBrightness())
        - (this.pixelD1.calculateBrightness() + (2 * this.pixel10.calculateBrightness())
            + this.pixel11.calculateBrightness()));
    t.checkExpect(pixel11.calculateVerticalEnergy(),
        (this.pixel00.calculateBrightness() + (2 * this.pixel01.calculateBrightness())
            + this.pixel02.calculateBrightness())
        - (this.pixel20.calculateBrightness() + (2 * this.pixel21.calculateBrightness())
            + this.pixel22.calculateBrightness()));
    t.checkExpect(pixel20.calculateVerticalEnergy(),
        (this.pixelD1.calculateBrightness() + (2 * this.pixel10.calculateBrightness())
            + this.pixel11.calculateBrightness())
        - (this.pixelcorner.calculateBrightness() + (2 * this.pixelR0.calculateBrightness())
            + this.pixelR1.calculateBrightness()));
  }

  // calculateEnergy()
  void testCalculateEnergy(Tester t) {
    this.initData();
    t.checkExpect(this.pixel00.calculateEnergy(),
        Math.sqrt(Math.pow(this.pixel00.calculateHorizontalEnergy(), 2.0)
            + Math.pow(this.pixel00.calculateVerticalEnergy(), 2.0)));
    t.checkExpect(this.pixel11.calculateEnergy(),
        Math.sqrt(Math.pow(this.pixel11.calculateHorizontalEnergy(), 2.0)
            + Math.pow(this.pixel11.calculateVerticalEnergy(), 2.0)));
    t.checkExpect(this.pixel20.calculateEnergy(),
        Math.sqrt(Math.pow(this.pixel20.calculateHorizontalEnergy(), 2.0)
            + Math.pow(this.pixel20.calculateVerticalEnergy(), 2.0)));
  }

  // expandHorizontally() test #1
  void testExpandHorizontally1(Tester t) {
    PixelSentinel corner = new PixelSentinel();
    t.checkExpect(corner.left, corner);
    t.checkExpect(corner.right, corner);

    corner.expandHorizontally(2);
    t.checkExpect(corner.left.left.left, corner);
    t.checkExpect(corner.right.right.right, corner);
  }

  //expandHorizontally() test #2
  void testExpandHorizontally2(Tester t) {
    PixelSentinel corner = new PixelSentinel();
    t.checkExpect(corner.left, corner);
    t.checkExpect(corner.right, corner);

    corner.expandHorizontally(3);
    t.checkExpect(corner.left.left.left.left, corner);
    t.checkExpect(corner.right.right.right.right, corner);
  }

  //render
  void testRender(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);
    ComputedPixelImage image = graph.render();

    t.checkExpect(image.getPixel(0, 2), this.pixel00.color);
    t.checkExpect(image.getPixel(1, 2), this.pixel01.color);
    t.checkExpect(image.getPixel(2, 2), this.pixel02.color);

    t.checkExpect(image.getPixel(0, 1), this.pixel10.color);
    t.checkExpect(image.getPixel(1, 1), this.pixel11.color);
    t.checkExpect(image.getPixel(2, 1), this.pixel12.color);

    t.checkExpect(image.getPixel(0, 0), this.pixel20.color);
    t.checkExpect(image.getPixel(1, 0), this.pixel21.color);
    t.checkExpect(image.getPixel(2, 0), this.pixel22.color);
  }
  
  //render
  void testRenderEnergy(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);
    ComputedPixelImage image = graph.renderEnergy();

    t.checkExpect(image.getPixel(0, 2), new Color(107, 107, 107));
    t.checkExpect(image.getPixel(1, 2), new Color(56, 56, 56));
    t.checkExpect(image.getPixel(2, 2), new Color(107, 107, 107));

    t.checkExpect(image.getPixel(0, 1), new Color(107, 107, 107));
    t.checkExpect(image.getPixel(1, 1), new Color(28, 28, 28));
    t.checkExpect(image.getPixel(2, 1), new Color(107, 107, 107));

    t.checkExpect(image.getPixel(0, 0), new Color(82, 82, 82));
    t.checkExpect(image.getPixel(1, 0), new Color(56, 56, 56));
    t.checkExpect(image.getPixel(2, 0), new Color(82, 82, 82));
  }


  // LeastEnergySeam() vertical
  void testLeastEnergySeamVertical(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);
    SeamInfo firstRemovedSeam = graph.leastEnergySeam(true);

    t.checkExpect(firstRemovedSeam.x, 1);
    t.checkExpect(firstRemovedSeam.y, 2);
    t.checkExpect(firstRemovedSeam.cameFrom.x, 1);
    t.checkExpect(firstRemovedSeam.cameFrom.y, 1);
    t.checkExpect(firstRemovedSeam.cameFrom.cameFrom.x, 1);
    t.checkExpect(firstRemovedSeam.cameFrom.cameFrom.y, 0);
  }
  
  // LeastEnergySeam() horizontal
  void testLeastEnergySeamHorizontal(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(new FromFileImage("images/33ex1.png"));
    SeamInfo firstRemovedSeam = graph.leastEnergySeam(false);

    t.checkExpect(firstRemovedSeam.x, 2);
    t.checkExpect(firstRemovedSeam.y, 2);
    t.checkExpect(firstRemovedSeam.cameFrom.x, 1);
    t.checkExpect(firstRemovedSeam.cameFrom.y, 1);
    t.checkExpect(firstRemovedSeam.cameFrom.cameFrom.x, 0);
    t.checkExpect(firstRemovedSeam.cameFrom.cameFrom.y, 2);
  }


  // makeFirstRowAsSeamInfo()
  void testMakeFirstRowAsSeamInfo(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);
    ArrayList<SeamInfo> firstRowSeams = graph.makeFirstRowAsSeamInfo(true);

    APixel row = graph.corner.down;
    APixel pixelFromGraph = row.right; 

    t.checkExpect(firstRowSeams.size(), 3);
    t.checkExpect(firstRowSeams.get(0).cameFrom, null);
    t.checkExpect(firstRowSeams.get(1).cameFrom, null);
    t.checkExpect(firstRowSeams.get(2).cameFrom, null);

    t.checkInexact(firstRowSeams.get(0).curr.calculateEnergy(), 
        ((Pixel) pixelFromGraph).calculateEnergy(), 0.0001);
    t.checkInexact(firstRowSeams.get(1).curr.calculateEnergy(), 
        ((Pixel) pixelFromGraph.right).calculateEnergy(), 0.0001);
    t.checkInexact(firstRowSeams.get(2).curr.calculateEnergy(), 
        ((Pixel) pixelFromGraph.right.right).calculateEnergy(), 0.0001);

    t.checkExpect(firstRowSeams.get(0).x, 0);
    t.checkExpect(firstRowSeams.get(0).y, 0);
    t.checkExpect(firstRowSeams.get(1).x, 1);
    t.checkExpect(firstRowSeams.get(1).y, 0);
    t.checkExpect(firstRowSeams.get(2).x, 2);
    t.checkExpect(firstRowSeams.get(2).y, 0);
  }
  
  // makeFirstRowAsSeamInfo()
  void testMakeFirstRowAsSeamInfoHorizontal(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(new FromFileImage("images/33ex1.png"));
    ArrayList<SeamInfo> firstRowSeams = graph.makeFirstRowAsSeamInfo(false);

    APixel column = graph.corner.right;
    APixel pixelFromGraph = column.down; 

    t.checkExpect(firstRowSeams.size(), 3);
    t.checkExpect(firstRowSeams.get(0).cameFrom, null);
    t.checkExpect(firstRowSeams.get(1).cameFrom, null);
    t.checkExpect(firstRowSeams.get(2).cameFrom, null);

    t.checkInexact(firstRowSeams.get(0).curr.calculateEnergy(), 
        ((Pixel) pixelFromGraph).calculateEnergy(), 0.0001);
    t.checkInexact(firstRowSeams.get(1).curr.calculateEnergy(), 
        ((Pixel) pixelFromGraph.down).calculateEnergy(), 0.0001);
    t.checkInexact(firstRowSeams.get(2).curr.calculateEnergy(), 
        ((Pixel) pixelFromGraph.down.down).calculateEnergy(), 0.0001);

    t.checkExpect(firstRowSeams.get(0).x, 0);
    t.checkExpect(firstRowSeams.get(0).y, 0);
    t.checkExpect(firstRowSeams.get(1).x, 0);
    t.checkExpect(firstRowSeams.get(1).y, 1);
    t.checkExpect(firstRowSeams.get(2).x, 0);
    t.checkExpect(firstRowSeams.get(2).y, 2);
  }

  // removeSeam()
  void testRemoveLeastEnergySeam(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);
    SeamInfo firstRemovedSeam = graph.leastEnergySeam(true);
    graph.removeSeam(firstRemovedSeam, true);

    t.checkExpect(this.pixel00.right, this.pixel02);
    t.checkExpect(this.pixel10.right, this.pixel12);
    t.checkExpect(this.pixel20.right, this.pixel22);
    t.checkExpect(this.pixel02.left, this.pixel00);
    t.checkExpect(this.pixel12.left, this.pixel10);
    t.checkExpect(this.pixel22.left, this.pixel20);
  }

  void testRemoveLeastEnergySeamHorizontal(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(new FromFileImage("images/33ex1.png"));
    PixelGraph removedGraph = new PixelGraph(new FromFileImage("images/32ex1.png"));
    SeamInfo firstRemovedSeam = graph.leastEnergySeam(false);
    graph.removeSeam(firstRemovedSeam, false);
    
    t.checkExpect(graph, removedGraph);
  }
  
  // makeSeamRed()
  void testMakeSeamRed(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);
    ComputedPixelImage image = graph.render();

    t.checkExpect(image.getPixel(1, 2), this.pixel01.color);
    t.checkExpect(image.getPixel(1, 1), this.pixel11.color);
    t.checkExpect(image.getPixel(1, 0), this.pixel21.color);

    SeamInfo seamToMakeRed = graph.leastEnergySeam(true);
    graph.makeSeamRed(seamToMakeRed);

    image = graph.render();
    t.checkExpect(image.getPixel(1, 2), Color.RED);
    t.checkExpect(image.getPixel(1, 1), Color.RED);
    t.checkExpect(image.getPixel(1, 0), Color.RED);
  }

  // lowerEnergy()
  void testLowerEnergy(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);
    ComputedPixelImage image = graph.render();

    SeamInfo firstRemovedSeam = graph.leastEnergySeam(true);
    graph.removeSeam(firstRemovedSeam, true);
    SeamInfo secondRemovedSeam = graph.leastEnergySeam(true);
    graph.removeSeam(secondRemovedSeam, true);

    // vertical seams
    SeamInfo testSeam1Start = new SeamInfo(this.pixel00, null, 0, 2);
    SeamInfo testSeam1Mid = new SeamInfo(this.pixel10, testSeam1Start, 0, 1);
    SeamInfo testSeam1 = new SeamInfo(this.pixel20, testSeam1Mid, 0, 0);

    SeamInfo testSeam2Start = new SeamInfo(this.pixel02, null, 2, 2);
    SeamInfo testSeam2Mid = new SeamInfo(this.pixel12, testSeam2Start, 2, 1);
    SeamInfo testSeam2 = new SeamInfo(this.pixel22, testSeam1Mid, 2, 0);

    // diagonal seams
    SeamInfo testSeam3Start = new SeamInfo(this.pixel01, null, 1, 2);
    SeamInfo testSeam3Mid = new SeamInfo(this.pixel11, testSeam3Start, 1, 1);
    SeamInfo testSeam3 = new SeamInfo(this.pixel22, testSeam3Mid, 2, 0);

    SeamInfo testSeam4Start = new SeamInfo(this.pixel01, null, 1, 2);
    SeamInfo testSeam4Mid = new SeamInfo(this.pixel11, testSeam3Start, 1, 1);
    SeamInfo testSeam4 = new SeamInfo(this.pixel20, testSeam3Mid, 0, 0);

    t.checkExpect(firstRemovedSeam.lowerEnergy(secondRemovedSeam), firstRemovedSeam); 
    t.checkExpect(testSeam1.lowerEnergy(testSeam2), testSeam2);
    t.checkExpect(testSeam3.lowerEnergy(testSeam4), testSeam3);
  }

  // deleteThisSeam()
  void testDeleteThisSeam(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);

    SeamInfo seamToRemove = graph.leastEnergySeam(true);

    Pixel bottomOfSeam = seamToRemove.curr;
    Pixel middleOfSeam = seamToRemove.cameFrom.curr;
    Pixel topOfSeam = seamToRemove.cameFrom.cameFrom.curr;

    APixel topOfSeamLeftNeighbor = topOfSeam.left;
    APixel topOfSeamRightNeighbor = topOfSeam.right;

    APixel midOfSeamLeftNeighbor = middleOfSeam.left;
    APixel midOfSeamRightNeighbor = middleOfSeam.right;

    APixel bottomOfSeamLeftNeighbor = bottomOfSeam.left;
    APixel bottomOfSeamRightNeighbor = bottomOfSeam.right;

    seamToRemove.deleteThisSeam(true);

    t.checkExpect(bottomOfSeam.right, bottomOfSeamRightNeighbor);
    t.checkExpect(bottomOfSeam.left, bottomOfSeamLeftNeighbor);

    t.checkExpect(middleOfSeam.right, midOfSeamRightNeighbor);
    t.checkExpect(middleOfSeam.left, midOfSeamLeftNeighbor);

    t.checkExpect(topOfSeam.right, topOfSeamRightNeighbor);
    t.checkExpect(topOfSeam.left, topOfSeamLeftNeighbor);
  }
  
  void testDeleteThisSeamHorizontal(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(new FromFileImage("images/33ex1.png"));
    SeamInfo seamToRemove = graph.leastEnergySeam(false);
    
    Pixel leftOfSeam = seamToRemove.curr;
    Pixel middleOfSeam = seamToRemove.cameFrom.curr;
    Pixel rightOfSeam = seamToRemove.cameFrom.cameFrom.curr;

    APixel leftOfSeamDownNeighbor = leftOfSeam.down;
    APixel leftOfSeamUpNeighbor = leftOfSeam.up;

    APixel midOfSeamDownNeighbor = middleOfSeam.down;
    APixel midOfSeamUpNeighbor = middleOfSeam.up;

    APixel rightOfSeamDownNeighbor = rightOfSeam.down;
    APixel rightOfSeamUpNeighbor = rightOfSeam.up;
    
    seamToRemove.deleteThisSeam(true);

    t.checkExpect(leftOfSeam.down, leftOfSeamDownNeighbor);
    t.checkExpect(leftOfSeam.up, leftOfSeamUpNeighbor);

    t.checkExpect(middleOfSeam.down, midOfSeamDownNeighbor);
    t.checkExpect(middleOfSeam.up, midOfSeamUpNeighbor);

    t.checkExpect(rightOfSeam.down, rightOfSeamDownNeighbor);
    t.checkExpect(rightOfSeam.up, rightOfSeamUpNeighbor);
  }

  // showSeam()
  void testShowSeam(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(this.pixelcorner, 3, 3);
    ComputedPixelImage image = graph.render();

    t.checkExpect(image.getPixel(1, 2), this.pixel01.color);
    t.checkExpect(image.getPixel(1, 1), this.pixel11.color);
    t.checkExpect(image.getPixel(1, 0), this.pixel21.color);

    graph.leastEnergySeam(true).showSeam();
    image = graph.render();

    t.checkExpect(image.getPixel(1, 2), Color.RED);
    t.checkExpect(image.getPixel(1, 1), Color.RED);
    t.checkExpect(image.getPixel(1, 0), Color.RED);
  }

  // calculatePosition() vertical
  void testCalcualtePositionVertical(Tester t) {
    PixelGraph graph = new PixelGraph(new FromFileImage("images/testImage.png"));
    SeamInfo leastEnergySeam = graph.leastEnergySeam(true);

    t.checkExpect(leastEnergySeam.calculatePosition(true), 0);
    t.checkExpect(leastEnergySeam.cameFrom.calculatePosition(true), 0);
  }

  //calculatePosition() horizontal
  void testCalcualtePositionHorizontal(Tester t) {
    PixelGraph graph = new PixelGraph(new FromFileImage("images/testImage.png"));
    SeamInfo leastEnergySeam = graph.leastEnergySeam(false);

    t.checkExpect(leastEnergySeam.calculatePosition(false), 0);
    t.checkExpect(leastEnergySeam.cameFrom.calculatePosition(false), 0);
  }

  // onTick()
  void testOnTick(Tester t) {
    this.initData();
    PixelGraph graph = new PixelGraph(new FromFileImage("images/testImage.png"));
    ComputedPixelImage image = graph.render();
    CarvingWorld world = new CarvingWorld(graph);
    int worldWidth = graph.width;
    int worldHeight = graph.height;

    t.checkExpect(world.tickCounter, 0);
    t.checkExpect(world.graph.width, worldWidth);

    world.onTick();

    t.checkExpect(world.tickCounter, 1);
    t.checkExpect(world.graph.width, worldWidth);

    world.onTick();

    t.checkExpect(world.tickCounter, 2);
    t.checkExpect((world.graph.width == worldWidth - 1) || (world.graph.height == worldHeight - 1),
        true);
  }
}
