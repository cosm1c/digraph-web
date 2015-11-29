'use strict';

var module = angular.module('digraph', []);

var colorsForState = {
    unknown: ['grey', 'grey'],
    stopped: ['#d9534f', '#d43f3a'],
    running: ['#5cb85c', '#4cae4c']
};

/*
 * Websocket messages:  ["eventName", {DATA}]
 */
module.factory('websocketFactory', ['$rootScope', function ($rootScope) {

    return function () {

        var ws = new WebSocket('ws://' + document.location.host + '/websocket');

        /**
         * @private
         * @dict
         */
        var callbacks = Object.create(null);

        ws.onmessage = function (evt) {
            //console.debug('onmessage', evt);
            var data = JSON.parse(evt.data);

            if (!angular.isArray(data)) {
                console.error('Malformed Websocket frame - not an array', data);
                return;
            }

            var eventName = data[0];
            if (!angular.isString(eventName)) {
                console.error('Malformed Websocket frame - eventName not a string', eventName, data);
                return;
            }

            var callback = callbacks[eventName];
            if (!angular.isDefined(callback)) {
                console.warn('No callback registered for eventName', eventName);
                return;
            }

            $rootScope.$apply(function () {
                callback(data[1]);
            });
        };

        ws.onerror = function (evt) {
            console.error("WebSocket error", evt);
            $rootScope.$apply();
        };

        ws.onopen = function (evt) {
            console.info("Websocket connected", evt);
            $rootScope.$apply();
        };

        ws.onclose = function (evt) {
            console.info("WebSocket disconnected", evt);
            $rootScope.$apply();
        };

        return {
            /**
             * @type {boolean}
             */
            isConnected: function () {
                return ws.readyState === 1;
            },

            /**
             * Register a listener for a type.
             * @param {!string} eventName The value of the type field on the received Websocket frame.
             * @param {!function(Object)} callback Callback function.
             */
            registerCallbackForEvent: function onType(eventName, callback) {
                if (!angular.isString(eventName)) {
                    console.error('eventName not a string', eventName);
                    return;
                }
                if (!angular.isFunction(callback)) {
                    console.error('Callback not a function for eventName', eventName, callback);
                    return;
                }
                if (callbacks[eventName]) {
                    console.warn('Overwriting existing callback for action', eventName);
                }
                callbacks[eventName] = callback;
            },

            /**
             * Sends a message to server over websocket.
             * @param {!string} message message to send
             */
            send: function (message) {
                if (!angular.isString(message)) {
                    console.error('message to send was not a string', message);
                }
                ws.send(message);
            }
        };
    };
}]);

module.filter('toArray', function () {
    return function (obj, addKey) {
        if (!angular.isObject(obj)) return obj;
        if (addKey === false) {
            return Object.keys(obj).map(function (key) {
                return obj[key];
            });
        } else {
            return Object.keys(obj).map(function (key) {
                var value = obj[key];
                return angular.isObject(value) ?
                    Object.defineProperty(value, '$key', {enumerable: false, value: key}) :
                {$key: key, $value: value};
            });
        }
    };
});

// use a factory instead of a directive, because cy.js is not just for visualisation; need access to the graph model and events etc
module.factory('nodeGraphFactory', ['$q', function ($q) {
    return function () {
        var deferred = $q.defer();

        $(function () { // on dom ready

            var cy = cytoscape({
                container: $('#cy')[0],

                userZoomingEnabled: false,

                style: cytoscape.stylesheet()
                    .selector('node')
                    .css({
                        'content': 'data(id)',
                        'height': 80,
                        'width': 80,
                        'text-valign': 'center',
                        'color': '#fff',
                        'text-outline-width': 2,
                        'text-outline-color': '#888'
                    })
                    .selector('edge')
                    .css({
                        'target-arrow-shape': 'triangle'
                    })
                    .selector('.unknown')
                    .css({
                        'background-color': colorsForState['unknown'][0],
                        'line-color': colorsForState['unknown'][1],
                        'target-arrow-color': colorsForState['unknown'][1],
                        'source-arrow-color': colorsForState['unknown'][1]
                    })
                    .selector('.stopped')
                    .css({
                        'background-color': colorsForState['stopped'][0],
                        'line-color': colorsForState['stopped'][1],
                        'target-arrow-color': colorsForState['stopped'][1],
                        'source-arrow-color': colorsForState['stopped'][1]
                    })
                    .selector('.running')
                    .css({
                        'background-color': colorsForState['running'][0],
                        'line-color': colorsForState['running'][1],
                        'target-arrow-color': colorsForState['running'][1],
                        'source-arrow-color': colorsForState['running'][1]
                    })
                    .selector(':selected')
                    .css({
                        'line-color': 'black',
                        'background-blacken': 0.25,
                        'text-outline-color': 'black'
                    }),

                layout: {
                    name: 'breadthfirst',
                    directed: true,
                    padding: 10
                },

                elements: [],

                ready: function () {
                    deferred.resolve(this);
                }
            });

        }); // on dom ready

        return deferred.promise;
    };
}]);

module.directive('nodeListItem', function () {

    function link(scope, element, attrs, controller, transcludeFn) {

        var node = scope.node;
        scope.nodeId = node.id;

        var logTerminal = element.find('logTerminal');

        scope.$on('logEntry', function (event, sourceNodeId, line) {
            if (sourceNodeId == node.id) {
                logTerminal.append('<samp>' + line + '</samp><br/>');
                logTerminal.get(0).scrollTop = logTerminal.get(0).scrollHeight;
            }
        });
        scope.$on('click', function (event, node) {
            console.log('selected');
            scope.$broadcast('nodeSelected', node.id);
        });
        scope.$on('nodeSelected', function (event, sourceNodeId) {
            if (node.id == sourceNodeId) {
                element.addClass('active');
            }
        });
        scope.$on('nodeUnselected', function (event, sourceNodeId) {
            if (node.id == sourceNodeId) {
                element.removeClass('active');
            }
        });
        scope.$on('expandAll', function () {
            console.log('expandAll', node.id);
            angular.element(document.getElementById('node-' + node.id)).collapse('show');
        });
        scope.$on('collapseAll', function () {
            console.log('collapseAll', node.id);
            angular.element(document.getElementById('node-' + node.id)).collapse('hide');
        });

        element.on('show.bs.collapse', function () {
            scope.subscribeNode();
        });
        element.on('hidden.bs.collapse', function () {
            scope.unsubscribeNode();
            logTerminal.empty();
        });
    }

    return {
        templateUrl: '/assets/node-list-item.html',
        link: link,
        scope: {
            node: '=',
            startNode: '&',
            stopNode: '&',
            subscribeNode: '&',
            unsubscribeNode: '&'
        }
    };
});

module.controller('digraphCtrl', ['$scope', 'websocketFactory', 'nodeGraphFactory', function ($scope, websocketFactory, nodeGraphFactory) {
    $scope.websocketConnected = function () {
        return false;
    };

    $scope.nodes = {};

    $scope.expandAll = function () {
        $scope.$broadcast('expandAll');
    };

    $scope.collapseAll = function () {
        $scope.$broadcast('collapseAll');
    };

    nodeGraphFactory().then(function (cy) {

        function addNodes(nodes) {
            nodes.forEach(function (node) {
                $scope.nodes[node.id] = node;
            });
            cy.batch(function () {
                var toAdd = {
                    nodes: [],
                    edges: []
                };
                nodes.forEach(function (node) {
                    if (cy.getElementById(node.id).length === 0) {
                        toAdd.nodes.push({
                            data: {id: node.id}
                        });
                        node.depends.forEach(function (targetId) {
                            toAdd.edges.push({
                                data: {
                                    source: node.id,
                                    target: targetId
                                }
                            });
                        });
                    }
                });
                cy.add(toAdd);
            });
            cy.layout();
            //cy.fit();

            updateNodes(nodes);
        }

        function setState(elem, stateClass) {
            elem.removeClass('running');
            elem.removeClass('stopped');
            elem.removeClass('unknown');
            elem.addClass(stateClass);
        }

        function updateNodes(nodeUpdates) {
            cy.batch(function () {
                nodeUpdates.forEach(function (nodeState) {
                    var node = cy.getElementById(nodeState.id);
                    $scope.nodes[nodeState.id].state = nodeState.state;

                    var neighbourhood = node.connectedEdges().add(node);
                    setState(neighbourhood, nodeState.state);
                });
            });
        }

        function logForNode(logEntry) {
            $scope.$broadcast('logEntry', logEntry.nodeId, logEntry.line)
        }

        var websocket = websocketFactory();

        $scope.websocketConnected = websocket.isConnected;

        websocket.registerCallbackForEvent('add', addNodes);
        websocket.registerCallbackForEvent('update', updateNodes);
        websocket.registerCallbackForEvent('log', logForNode);

        cy.on('select', function (evt) {
            $scope.$broadcast('nodeSelected', evt.cyTarget.data('id'));
        });

        cy.on('unselect', function (evt) {
            $scope.$broadcast('nodeUnselected', evt.cyTarget.data('id'));
        });

        $scope.nodeClicked = function (nodeId) {
            cy.batch(function () {
                cy.nodes().unselect();
                cy.$('#' + nodeId).select();
            });
        };

        $scope.startNode = function (nodeId) {
            websocket.send(JSON.stringify(['startNode', nodeId]));
        };

        $scope.stopNode = function (nodeId) {
            websocket.send(JSON.stringify(['stopNode', nodeId]));
        };

        $scope.subscribeNode = function (nodeId) {
            websocket.send(JSON.stringify(['subscribeNode', nodeId]));
        };

        $scope.unsubscribeNode = function (nodeId) {
            websocket.send(JSON.stringify(['unsubscribeNode', nodeId]));
        };
    });
}]);


angular.element(document).ready(function () {
    angular.bootstrap(document, ['digraph']);
});
