// Copyright 2015-present 650 Industries. All rights reserved.

import UIKit

/**
 This class is based on https://gist.github.com/darrarski/29a2a4515508e385c90b3ffe6f975df7
 */
final class EXBlurEffectView: UIVisualEffectView {
  @Clamping(lowerBound: 0.01, upperBound: 1) var intensity: Float = 0.5 {
    didSet {
      setNeedsDisplay()
    }
  }

  @Containing(values: ["default", "light", "dark"]) var tint = "default" {
    didSet {
      visualEffect = UIBlurEffect(tint: tint)
    }
  }

  private var visualEffect: UIVisualEffect = UIBlurEffect(tint: "default") {
    didSet {
      setNeedsDisplay()
    }
  }
  private var animator: UIViewPropertyAnimator?

  init() {
    super.init(effect: nil)
  }

  required init?(coder aDecoder: NSCoder) { nil }

  deinit {
    animator?.stopAnimation(true)
  }

  override func draw(_ rect: CGRect) {
    super.draw(rect)
    effect = nil
    animator?.stopAnimation(true)
    animator = UIViewPropertyAnimator(duration: 1, curve: .linear) { [unowned self] in
      self.effect = visualEffect
    }
    animator?.fractionComplete = CGFloat(intensity)
  }
}

/**
 Property wrapper clamping the value between an upper and lower bound
 */
@propertyWrapper
struct Clamping<Value: Comparable> {
  var wrappedValue: Value

  init(wrappedValue: Value, lowerBound: Value, upperBound: Value) {
    self.wrappedValue = max(lowerBound, min(upperBound, wrappedValue))
  }
}

/**
 Property wrapper ensuring that the value is contained in list of valid values
 */
@propertyWrapper
struct Containing<Value: Equatable> {
  var wrappedValue: Value

  init(wrappedValue: Value, values: Array<Value>) {
    let isValueValid = values.contains(wrappedValue)
    self.wrappedValue = isValueValid ? wrappedValue : values.first!
  }
}
