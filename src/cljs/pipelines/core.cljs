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

(defonce pipeline (atom "echo"))

(defn get-state [k & [default]]
  (clojure.core/get @app-state k default))

(defn put! [k v]
  (swap! app-state assoc k v))

(defn main-page []
  [:div [(get-state :current-page)]])

(defn edit-pipeline []
  [:div.text-input.box.
   [:h2 "Function"]
   [:input {:value @pipeline
            :on-change #(reset! pipeline (-> % .-target .-value))}]])

(defn text-input []
  [:div.text-input.box.left
   [:h2 (str "Input")]
   [:textarea {:value @input
               :style { :width "90%" :height "200px"  :overflow-y "scroll" }
               :on-change #(reset! input (-> % .-target .-value))}]])

(defn results []
  (let [in @input
        p @pipeline]
  [:div
   [:h2 "Pipeline"]
   (for [piece (partition 2 (interleave
                             (clojure.string/split p #"\s*\|\s*")
                             (pipes/compute-pipeline p in)))]
     [:div
      [:h3 (str (first piece))]
      [:textarea {:value (last piece)
                  :readOnly true
                  :style { :width "90%" :height "200px" :overflow-y "scroll" }}]])]))

(defn page1 []
  [:div
   [edit-pipeline]
   [text-input]
   [results]])

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
