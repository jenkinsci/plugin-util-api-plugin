def configurations = [
  [ platform: "linux", jdk: 25 ],
  [ platform: "windows", jdk: 21 ]
]

buildPlugin(failFast: false, configurations: configurations,
    checkstyle: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]],
    pmd: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]] )
