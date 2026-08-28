import sys

target = """            val chartPoints = remember(startMillis, positions, fds, unitTrusts, crypto, otherInvestments) {
                val step = (now - startMillis) / numPoints.coerceAtLeast(1)
                var maxVal = 0.0
                var minVal = Double.MAX_VALUE
                val rawValues = mutableListOf<Double>()
                
                for (i in 0..numPoints) {
                    val t = startMillis + i * step
                    var valueAtT = 0.0
                    
                    positions.forEach { p ->
                        if (t >= p.purchaseDate) {
                            val progress = (t - p.purchaseDate).toDouble() / (now - p.purchaseDate).coerceAtLeast(1L)
                            valueAtT += (p.averagePrice + (p.currentPrice - p.averagePrice) * progress) * p.quantity
                        }
                    }
                    fds.forEach { fd ->
                        if (t >= fd.startDate) {
                            val progress = (t - fd.startDate).toDouble() / (now - fd.startDate).coerceAtLeast(1L)
                            valueAtT += fd.principalAmount + (fd.currentValue - fd.principalAmount) * progress
                        }
                    }
                    unitTrusts.forEach { ut ->
                        if (t >= ut.purchaseDate) {
                            val progress = (t - ut.purchaseDate).toDouble() / (now - ut.purchaseDate).coerceAtLeast(1L)
                            val currentNav = if (ut.currentNav > 0) ut.currentNav else ut.averageNav
                            valueAtT += (ut.averageNav + (currentNav - ut.averageNav) * progress) * ut.units
                        }
                    }
                    crypto.forEach { c ->
                        if (t >= c.purchaseDate) {
                            val progress = (t - c.purchaseDate).toDouble() / (now - c.purchaseDate).coerceAtLeast(1L)
                            val currentPrice = if (c.currentPrice > 0) c.currentPrice else c.averagePrice
                            valueAtT += (c.averagePrice + (currentPrice - c.averagePrice) * progress) * c.quantity
                        }
                    }
                    otherInvestments.forEach { o ->
                        if (t >= o.purchaseDate) {
                            valueAtT += o.value
                        }
                    }
                    
                    rawValues.add(valueAtT)
                    if (valueAtT > maxVal) maxVal = valueAtT
                    if (valueAtT < minVal) minVal = valueAtT
                }
                
                val range = maxVal - minVal
                if (range == 0.0) {
                    List(numPoints + 1) { 0.5f }
                } else {
                    rawValues.map { (1.0 - (it - minVal) / range).toFloat() }
                }
            }"""

replacement = """            val chartData = remember(startMillis, positions, fds, unitTrusts, crypto, otherInvestments) {
                val step = (now - startMillis) / numPoints.coerceAtLeast(1)
                var maxVal = 0.0
                var minVal = Double.MAX_VALUE
                val rawValues = mutableListOf<Double>()
                
                for (i in 0..numPoints) {
                    val t = startMillis + i * step
                    var valueAtT = 0.0
                    
                    positions.forEach { p ->
                        if (t >= p.purchaseDate) {
                            val progress = (t - p.purchaseDate).toDouble() / (now - p.purchaseDate).coerceAtLeast(1L)
                            valueAtT += (p.averagePrice + (p.currentPrice - p.averagePrice) * progress) * p.quantity
                        }
                    }
                    fds.forEach { fd ->
                        if (t >= fd.startDate) {
                            val progress = (t - fd.startDate).toDouble() / (now - fd.startDate).coerceAtLeast(1L)
                            valueAtT += fd.principalAmount + (fd.currentValue - fd.principalAmount) * progress
                        }
                    }
                    unitTrusts.forEach { ut ->
                        if (t >= ut.purchaseDate) {
                            val progress = (t - ut.purchaseDate).toDouble() / (now - ut.purchaseDate).coerceAtLeast(1L)
                            val currentNav = if (ut.currentNav > 0) ut.currentNav else ut.averageNav
                            valueAtT += (ut.averageNav + (currentNav - ut.averageNav) * progress) * ut.units
                        }
                    }
                    crypto.forEach { c ->
                        if (t >= c.purchaseDate) {
                            val progress = (t - c.purchaseDate).toDouble() / (now - c.purchaseDate).coerceAtLeast(1L)
                            val currentPrice = if (c.currentPrice > 0) c.currentPrice else c.averagePrice
                            valueAtT += (c.averagePrice + (currentPrice - c.averagePrice) * progress) * c.quantity
                        }
                    }
                    otherInvestments.forEach { o ->
                        if (t >= o.purchaseDate) {
                            valueAtT += o.value
                        }
                    }
                    
                    rawValues.add(valueAtT)
                    if (valueAtT > maxVal) maxVal = valueAtT
                    if (valueAtT < minVal) minVal = valueAtT
                }
                
                val range = maxVal - minVal
                val points = if (range == 0.0) {
                    List(numPoints + 1) { 0.5f }
                } else {
                    rawValues.map { (1.0 - (it - minVal) / range).toFloat() }
                }
                Triple(points, minVal, maxVal)
            }
            val chartPoints = chartData.first
            val chartMin = chartData.second
            val chartMax = chartData.third"""

with open("app/src/main/java/com/example/DashboardScreen.kt", "r") as f:
    content = f.read()

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/DashboardScreen.kt", "w") as f:
    f.write(content)
