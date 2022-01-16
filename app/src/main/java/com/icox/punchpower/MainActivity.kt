package com.icox.punchpower

import android.animation.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // 측정된 최대 펀치력
    var maxPower = 0.0

    // 펀치력 측정이 시작되었는지 나타내는 변수
    var isStart = false

    // 펀치력 측정이 시작된 시간
    var startTime = 0L

    // SensorManager 객체 -> lazy 로 실제 사용될 때 초기화 한다
    val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // SensorEventListener
    val eventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
//            측정된 센서 값이 TYPE_LINEAR_ACCELERATION 이 아니면 바로 리턴
                if (event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return@let

//            각 좌표 값을 제곱하여 음수 값을 없애고, 값의 차이를 극대화
                val power = Math.pow(event.values[0].toDouble(), 2.0) + Math.pow(
                    event.values[1].toDouble(),
                    2.0
                ) + Math.pow(event.values[2].toDouble(), 2.0)

//            측정된 펀치력이 20을 넘고 아직 측정이 시작되지 않은 경우
                if (power > 20 && !isStart) {
//                측정 시작
                    startTime = System.currentTimeMillis()
                    isStart = true
                }

//                측정이 시작된 경우
                if (isStart) {
//                    Animation 제거
                    imageView.clearAnimation()
//                    5초간 최댓값을 측정 -> 측정된 값이 기존 최댓값보다 크면 새로 갱신
                    if (maxPower < power)
                        maxPower = power
//                    측정 중인 것을 사용자에게 알려줌
                    stateLabel.text = "펀치력을 측정하고 있습니다"
//                    최초 측정 후 3초가 지났으면 측정을 끝낸다
                    if (System.currentTimeMillis() - startTime > 3000) {
                        isStart = false
                        punchPowerTestComplete(maxPower)
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    // 화면이 최초 생성될 때 호출되는 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    // Activity 가 사라졌다 다시 보일 때마다 호출되는 함수
    override fun onStart() {
        super.onStart()
        initGame()
    }

    // 게임 초기화
    fun initGame() {
        maxPower = 0.0
        isStart = false
        startTime = 0L
        stateLabel.text = "핸드폰을 손에 쥐고 주먹을 내지르세요"

//        센서의 변화 값을 처리할 Listener 를 등록한다
        sensorManager.registerListener(
            eventListener,
            // TYPE_LINEAR_ACCELERATION 은 중력 값을 제외하고 x, y, z축에 측정된 가속도만 계산되어 나온다
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
            SensorManager.SENSOR_DELAY_NORMAL
        )
//        imageView.startAnimation(AnimationUtils.loadAnimation(this@MainActivity, R.anim.translate))
//        imageView.startAnimation(AnimationUtils.loadAnimation(this@MainActivity, R.anim.rotate))

        /* imageView.startAnimation(
               AnimationUtils.loadAnimation(
                   this@MainActivity,
                   R.anim.alpha_scale
               )
           ) */

/* //        Animation 시작
        val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.alpha_scale)
        imageView.startAnimation(animation)

//        AnimationListener 설정
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationRepeat(animation: Animation?) {

            }
        }) */

//        property_animation 불러오기 -> apply 함수를 사용하면 로딩된 Animator 가 this 로 저장됨
        AnimatorInflater.loadAnimator(this@MainActivity, R.animator.property_animation).apply {
//            Animation 종료 Listener 추가
            addListener(object : AnimatorListenerAdapter() {
                // Animation 이 종료될 때 Animation 다시 시작
                override fun onAnimationEnd(animation: Animator?) {
                    start()
                }
            })
//            property_animation 의 타겟을 imageView 로 지정
            setTarget(imageView)
//            Animation 시작
            start()
        }

//        color_animation 불러오기 -> apply 함수를 사용하면 로딩된 Animator 가 this 로 저장됨
        AnimatorInflater.loadAnimator(this@MainActivity, R.animator.color_animation).apply {
//            color_animation 을 불러오고 ObjectAnimator 클래스로 캐스팅
            val colorAnimator = this@apply as? ObjectAnimator
//            colorAnimator 가 ObjectAnimator 인 경우에만 실행
            colorAnimator?.apply {
//                Evaluator 를 ArgbEvaluator()로 설정
                setEvaluator(ArgbEvaluator())
//                타겟을 Activity 의 ContentView 로 지정
                target = window.decorView.findViewById(android.R.id.content)
//                Animation 시작
                start()
            }
        }
    }

    // 펀치력 측정이 완료된 경우 처리 함수
    fun punchPowerTestComplete(power: Double) {
        Log.d("MainActivity", "측정완료: power ${String.format("%.5f", power)}")
        sensorManager.unregisterListener(eventListener)

        val intent = Intent(this@MainActivity, ResultActivity::class.java)
        intent.putExtra("power", power)
        startActivity(intent)
    }

    // Activity 가 화면에서 사라지면 호출되는 함수
    override fun onStop() {
        super.onStop()
        try {
            sensorManager.unregisterListener(eventListener)
        } catch (e: Exception) {

        }
    }

}