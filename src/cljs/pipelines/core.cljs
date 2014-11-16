(ns pipelines.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [pipelines.pipes :as pipes])
    (:use [clojure.string :only (join)])
    (:import goog.History))

;; -------------------------
;; State
(defonce app-state (atom {:text "Hello, this is: "}))

(defonce input (atom "Banana
Fruit
Apple
Zebra
Lemon
Garnish
Fruit Salad
Apple Pie
"))

(defonce curfunc (atom #(identity %)))

(def func-choices
  (sorted-map
   "echo" identity
   "sort" pipes/sort
   "uniq" pipes/uniq
   "wc" pipes/wc))

(defn choose-func
  [x]
  (func-choices
   x
   identity))

(defn get-state [k & [default]]
  (clojure.core/get @app-state k default))

(defn put! [k v]
  (swap! app-state assoc k v))

(defn main-page []
  [:div [(get-state :current-page)]])

(defn func-input []
  [:div.text-input.box.left
   [:h2 "Function"]
   [:select { :onChange #(reset! curfunc (choose-func (-> % .-target .-value))) }
    (for [k (keys func-choices)]
      [:option k])]])

(defn text-input []
  [:div.text-input.box.left
   [:h2 (str "Input")]
   [:textarea {:value @input
               :style { :width "90%" :height "200px"  :overflow-y "scroll" }
               :on-change #(reset! input (-> % .-target .-value))}]])

(defn output []
  (let [out (@curfunc @input)]
    [:div
     [:h2 "Output"]
     [:textarea {:value out
                 :readOnly true
                 :style { :width "90%" :height "200px" :overflow-y "scroll" }}]]))

(defn page1 []
  [:div
   [func-input]
   [text-input]
   [output]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (put! :current-page page1))

;; -------------------------
;; Initialize app
(defn init! []
  (reagent/render-component [main-page] (.getElementById js/document "app")))

;; -------------------------
;; History
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))
;; need to run this after routes have been defined
(hook-browser-navigation!)
